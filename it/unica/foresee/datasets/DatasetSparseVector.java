package it.unica.foresee.datasets;

import it.unica.foresee.datasets.interfaces.DatasetElement;
import org.apache.commons.math3.ml.clustering.Clusterable;

import java.util.*;

/**
 * An efficient data structure for sparse vectors.
 */
public class DatasetSparseVector<T extends DatasetElement<?>> extends TreeMap<Integer, T> implements it.unica.foresee.datasets.interfaces.DatasetVector<T>, it.unica.foresee.datasets.interfaces.DatasetElement<DatasetSparseVector<T>>, Clusterable
{
    /**
     * Mean of the elements means.
     */
    private double mean;

    /**
     * Max size of the vector
     */
    private int vectorSize;

    /**
     * Flag to check if the mean value is calculated or user selected.
     */
    private boolean meanValueSetByUser = false;


    /* Getter */

    /**
     * {@inheritDoc}
     */
    @Override
    public T getDatasetElement(int key)
    {
        return this.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DatasetSparseVector getElement() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DatasetSparseVector[] getKFoldPartitions(int k, int layersAmount)
    {
        /* Initialize the max and min with a reasonable value */
        double maxMeanValue = this.get(this.firstKey()).getValueForMean();
        double minMeanValue = this.get(this.firstKey()).getValueForMean();

        /* Fill the array with the number of occurrences. */
        for (T item : this.values())
        {
            /* Keep the max mean value for each element */
            if (item.getValueForMean() > maxMeanValue)
            {
                maxMeanValue = item.getValueForMean();
            }

            /* Keep the min mean value for each element */
            if (item.getValueForMean() < minMeanValue)
            {
                minMeanValue = item.getValueForMean();
            }
        }


        /* --- Stratification by mean value. --- */

        /* Amplitude of the range of each layer. */
        double layerRange = (maxMeanValue - minMeanValue) / (layersAmount);

        /*
         * Place the elements keys in the respective layers.
         *
         * In each loop take an element key and try to put it in a layer from the first until
         * the last is reached.
         * If the last layer is reached, add the element in it without further checks.
         */
        ArrayList<Integer>[] layers = new ArrayList[layersAmount];

        /* Initialize the Array */
        for (int i = 0; i < layers.length; i++)
        {
            /* Each layer contains only the keys referring to the element */
            layers[i] = new ArrayList<>();
        }

        /* Stratification loop */
        for (Integer key: this.keySet())
        {
            /* Reset the high range. */
            double highRange = minMeanValue + layerRange;

            for (ArrayList<Integer> layer: layers)
            {
                /* We're on the last layer, add here the element here. */
                if (layer.equals(layers[layersAmount - 1]))
                {
                    layer.add(key);
                    break; //Do not continue to check for a layer after it has been found
                }
                else if (this.get(key).getValueForMean() < highRange)
                {
                    layer.add(key);
                    break; //Do not continue to check for a layer after it has been found
                }

                /* Update the range. */
                highRange += layerRange;
            }
        }

        /* Fill the k partitions: k folding */
        DatasetSparseVector<T>[] partitions = new DatasetSparseVector[k];
        Random randomizer = new Random();

        /* Initialize the partitions */
        for (int i = 0; i < partitions.length; i++)
        {
            partitions[i] = new DatasetSparseVector<>();
        }

        /* For each layer add random elements to each partition */
        for (ArrayList<Integer> layer : layers)
        {
            /* Remove the elements added to the partitions */
            while (layer.size() > 0)
            {
                /* Select a random element and put it in a partition */
                for (DatasetSparseVector<T> partition : partitions)
                {
                    if (layer.size() <= 0)
                    {
                        /* Stop looping when the layer is empty */
                        break;
                    }
                    else
                    {
                        int randIndex = randomizer.nextInt(layer.size());
                        Integer randomKey = layer.remove(randIndex);
                        partition.put(randomKey, this.get(randomKey));
                    }
                }
            }
        }

        return partitions;
    }

    /**
     * Get the vector size to create an array
     * @return the vector size
     */
    public int getVectorSize() {
        return vectorSize;
    }

    /**
     * Checks if the mean value has been set by the user.
     *
     * @return true if the mean value has been set by the user
     */
    public boolean isMeanValueSetByUser() {
        return this.meanValueSetByUser;
    }

    /**
     * {@inheritDoc}
     *
     * The mean is calculated internally as the mean of the elements means. If a different value is set
     * by the user, it will be used instead.
     */
    @Override
    public double getValueForMean()
    {
        if (this.meanValueSetByUser)
            return this.mean;
        else
        {
            double sumOfMeans = 0;
            for (T element : this.values())
            {
                sumOfMeans += element.getValueForMean();
            }
            return sumOfMeans / this.size();
        }
    }

    /* Setter */

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDatasetElement(int index, T e) {
        this.put(index, e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setElement(DatasetSparseVector<T> e) {
        this.clear();
        this.putAll(e);
        this.meanValueSetByUser = e.isMeanValueSetByUser();
        if (this.meanValueSetByUser)
        {
            this.mean = e.getValueForMean();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This value overrides the internally calculated mean.
     */
    @Override
    public void setValueForMean(double v) {
        this.meanValueSetByUser = true;
        this.mean = v;
    }

    /**
     * Set the vector size for array creation
     * @param vectorSize the size of the max vector
     */
    public void setVectorSize(int vectorSize) {
        this.vectorSize = vectorSize;
    }

    /**
     * Unsets the mean set by the user. Successive calls of
     * {@link #getValueForMean()} will use the mean of the elements means.
     */
    public void unsetMeanValueSetByUser()
    {
        this.meanValueSetByUser = false;
        this.mean = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<T> iterator() {
        return this.values().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getPoint()
    {
        // The size of the array is set to the highest key value, so that it can store all the items
        double[] points = new double[getVectorSize()];

        if (this.isEmpty())
        {
            return points;
        }

        // Associate the indexes with the corresponding values
        for (int k : this.keySet())
        {
            points[k] = this.getDatasetElement(k).getValueForMean();
        }

        return points;
    }
}