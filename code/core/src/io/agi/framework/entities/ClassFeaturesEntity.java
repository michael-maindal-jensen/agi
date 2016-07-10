/*
 * Copyright (c) 2016.
 *
 * This file is part of Project AGI. <http://agi.io>
 *
 * Project AGI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Project AGI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Project AGI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.agi.framework.entities;

import io.agi.core.data.Data;
import io.agi.core.data.DataSize;
import io.agi.core.orm.ObjectMap;
import io.agi.framework.DataFlags;
import io.agi.framework.Entity;
import io.agi.framework.Framework;
import io.agi.framework.Node;
import io.agi.framework.persistence.models.ModelEntity;

import java.util.Collection;

/**
 * Associates bits in a binary vector with classifications.
 *
 * Inputs:
 *
 * - Class: A Data containing a Scalar integer value representing the current class
 * - Features: A Data containing a set of binary features (1 = present, 0 = absent)
 *
 * Outputs:
 *
 *  - ClassFeatures (A Data containing the frequencies of all features x all classes.)
 *
 *
 * Options are online learning or all-time learning.
 *
 * Created by dave on 8/07/16.
 */
public class ClassFeaturesEntity extends Entity {

    public static final String ENTITY_TYPE = "class-features";

    public static final String FEATURES = "features";
    public static final String FEATURE_CLASS_COUNT = "feature-class-count";
    public static final String CLASS_PREDICTION = "class-prediction";

    public ClassFeaturesEntity( ObjectMap om, Node n, ModelEntity model ) {
        super( om, n, model );
    }

    public void getInputAttributes( Collection< String > attributes ) {
        attributes.add( FEATURES );
    }

    public void getOutputAttributes( Collection< String > attributes, DataFlags flags ) {
        attributes.add( FEATURE_CLASS_COUNT );
        attributes.add( CLASS_PREDICTION );
    }

    @Override
    public Class getConfigClass() {
        return ClassFeaturesEntityConfig.class;
    }

    protected void doUpdateSelf() {

        ClassFeaturesEntityConfig config = ( ClassFeaturesEntityConfig ) _config;

        Data featureData = getData( FEATURES );
        if( featureData == null ) {
            return;
        }

        // Get the input classification
        String stringClassValue = Framework.GetConfig( config.classEntityName, config.classConfigPath );
        Integer classValue = Integer.valueOf( stringClassValue );
        if( classValue == null ) {
            classValue = 0;
        }

        // Get all the parameters:
        int features = featureData.getSize();
        DataSize dataSizeFeatures = DataSize.create( features * config.classes );
        Data featureClassCount = getDataLazyResize( FEATURE_CLASS_COUNT, dataSizeFeatures );
        Data classPrediction = getDataLazyResize( CLASS_PREDICTION, DataSize.create( config.classes ) );

        // update counts
        if( config.learn ) {
            for( int i = 0; i < features; ++i ) {
                float r = featureData._values[ i ];
                if( r == 0.f ) {
                    continue;
                }

                if( config.onlineLearning ) {
                    // adjust all classes based on the most recent observation
                    for( int c = 0; c < config.classes; ++c ) {
                        float delta = -1.f;
                        if( c == classValue ) {
                            delta = 1.f;
                        }

                        int offset = i * config.classes + c;

                        float oldCount = featureClassCount._values[ offset ];
                        float newCount = oldCount + delta;
                        newCount = Math.max( 0, newCount );
                        newCount = Math.min( config.onlineMaxCount, newCount );

//-bug in hebbian predictor:
//if frequency < 0.5 then will tend to zero
//if frequency > 0.5 then will tend to max
//this couldve badly affected results.

                        featureClassCount._values[ offset ] = newCount;
                    }
                }
                else {
                    int offset = i * config.classes + classValue;

                    featureClassCount._values[ offset ] += 1;
                }
            }

            setData( FEATURE_CLASS_COUNT, featureClassCount );
        }

        // predict:
        int activeFeatures = 0;

        classPrediction.set( 0.f );

        for( int i = 0; i < features; ++i ) {
            float r = featureData._values[ i ];
            if( r == 0.f ) {
                continue;
            }

            // active feature: so what does it vote for?
            ++activeFeatures;

            for( int c = 0; c < config.classes; ++c ) {
                int offset = i * config.classes + c;
                float count = featureClassCount._values[ offset ];
                classPrediction._values[ c ] += count;
            }
        }

        classPrediction.scaleSum( 1.f );
        setData( CLASS_PREDICTION, classPrediction );

        // calculate the result
        int maxAt = classPrediction.maxAt().offset();
        int error = 0;
        if( maxAt != classValue ) {
            error = 1;
        }

        // update the config based on the result:
        config.classPredicted = maxAt; // the predicted class given the input features
        config.classError = error; // 1 if the prediction didn't match the input class
        config.classTruth = classValue; // the value that was taken as input
    }

}
