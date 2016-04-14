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

package io.agi.framework.demo.classifier;

import io.agi.framework.Entity;
import io.agi.framework.Framework;
import io.agi.framework.Main;
import io.agi.framework.Node;
import io.agi.framework.entities.DiscreteRandomEntity;
import io.agi.framework.entities.DynamicSelfOrganizingMapEntity;
import io.agi.framework.entities.GrowingNeuralGasEntity;
import io.agi.framework.entities.RandomVectorEntity;
import io.agi.framework.factories.CommonEntityFactory;
import io.agi.framework.persistence.Persistence;

/**
 * Code to demonstrate a DSOM Entity on a simple test problem.
 * <p/>
 * Created by dave on 12/03/16.
 */
public class ClassifierDemo {

    public static void main( String[] args ) {

        // Provide classes for entities
        CommonEntityFactory ef = new CommonEntityFactory();

        // Create a Node
        Main m = new Main();
        m.setup( args[ 0 ], null, ef );

        // Create custom entities and references
        if( args.length > 1 ) {
            Framework.LoadEntities( args[ 1 ] );
        }

        if( args.length > 2 ) {
            Framework.LoadDataReferences( args[ 2 ] );
        }

        if( args.length > 3 ) {
            Framework.LoadConfigs( args[ 3 ] );
        }

        // Programmatic hook to create entities and references..
        createEntities( m._n );

        // Start the system
        m.run();
    }

    public static void createEntities( Node n ) {

        // Define some entities
        String modelName = "model";
        String classifierName = "classifier";

        Framework.CreateEntity( modelName, DiscreteRandomEntity.ENTITY_TYPE, n.getName(), null );
        Framework.CreateEntity( classifierName, GrowingNeuralGasEntity.ENTITY_TYPE, n.getName(), modelName );

        // Connect the entities
        Persistence p = n.getPersistence();

        Framework.SetDataReference( classifierName, DynamicSelfOrganizingMapEntity.INPUT, modelName, RandomVectorEntity.OUTPUT );

        // Set a property:
        Framework.SetConfig( modelName, "elements", "2" );
        Framework.SetConfig( classifierName, Entity.SUFFIX_RESET, "true" );
    }
}