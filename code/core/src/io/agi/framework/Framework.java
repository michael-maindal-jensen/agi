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

package io.agi.framework;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.agi.core.orm.NamedObject;
import io.agi.core.util.FileUtil;
import io.agi.framework.coordination.http.HttpExportHandler;
import io.agi.framework.persistence.Persistence;
import io.agi.framework.persistence.models.ModelData;
import io.agi.framework.persistence.models.ModelDataReference;
import io.agi.framework.persistence.models.ModelEntity;
import io.agi.framework.persistence.models.ModelEntityConfigPath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Functions used throughout the experimental framework, i.e. not specific to Entity or Node.
 * Created by dave on 2/04/16.
 */
public class Framework {

    protected static final Logger logger = LogManager.getLogger();

    /**
     * Modifies the database to make the reference _entityName-suffix Data a reference input to the input _entityName-suffix.
     *
     * @param inputEntity
     * @param inputSuffix
     * @param referenceEntity
     * @param referenceSuffix
     */
    public static void SetDataReference(
            String inputEntity,
            String inputSuffix,
            String referenceEntity,
            String referenceSuffix ) {
        String inputKey = NamedObject.GetKey( inputEntity, inputSuffix );
        String refKey = NamedObject.GetKey( referenceEntity, referenceSuffix );
        SetDataReference( inputKey, refKey );
    }

    /**
     * Modifies the database to make the reference _entityName-suffix Data a reference input to the input _entityName-suffix.
     *
     * @param dataKey
     * @param refKeys
     */
    public static void SetDataReference(
            String dataKey,
            String refKeys ) {
        Persistence persistence = Node.NodeInstance().getPersistence();
        ModelData modelData = persistence.fetchData( dataKey );

        if( modelData == null ) {
            modelData = new ModelData( dataKey, refKeys );
        }

        modelData.refKeys = refKeys;
        persistence.persistData( modelData );
    }

    /**
     * Set the data in the model, in the persistence layer.
     * If an entry exists for this key, replace it.
     *
     * @param modelData
     */
    public static void SetData( ModelData modelData ) {
        Persistence persistence = Node.NodeInstance().getPersistence();
        persistence.persistData( modelData );
    }

    /**
     * Allows a single config property to be obtained.
     *
     * @param entityName
     * @param configPath
     */
    public static String GetConfig( String entityName, String configPath ) {
        Persistence persistence = Node.NodeInstance().getPersistence();
        ModelEntity me = persistence.fetchEntity( entityName );
        JsonParser parser = new JsonParser();
        JsonObject jo = parser.parse( me.config ).getAsJsonObject();

        // navigate to the nested property
        JsonElement je = GetNestedProperty( jo, configPath );

        return je.getAsString();
    }

    /**
     * Gets the complete config object for the given entity.
     *
     * @param entityName
     * @return
     */
    public static String GetConfig( String entityName ) {
        Persistence persistence = Node.NodeInstance().getPersistence();
        ModelEntity me = persistence.fetchEntity( entityName );
        if( me == null ) {
            return null;
        }

        return me.config;
    }

    /**
     * Allows a single config property to be modified.
     */
    public static void SetConfig( String entityName, String configPath, String value ) {
        Persistence persistence = Node.NodeInstance().getPersistence();
        ModelEntity me = persistence.fetchEntity( entityName );
        JsonParser parser = new JsonParser();
        JsonObject root = parser.parse( me.config ).getAsJsonObject();

        // navigate to the nested property
        // N.B. String.split : http://stackoverflow.com/questions/3481828/how-to-split-a-string-in-java
        JsonObject parent = root;
        String[] pathParts = configPath.split( "[.]" );
        int index = 0;
        int maxIndex = pathParts.length - 1; // NOTE: one before the one we're looking for
        String part = null;
//        if( pathParts.length < 2 ) { // i.e. 0 or 1
//            part = configPath;
//        }

        while( index < maxIndex ) {
            part = pathParts[ index ];
            JsonElement child = parent.get( part );

            ++index;

            parent = ( JsonObject ) child;
        }

        part = pathParts[ index ];

        // replace the property:
        parent.remove( part );
        parent.addProperty( part, value );

        // re-serialize the whole thing
        me.config = root.toString();//getAsString();
        persistence.persistEntity( me );
    }

    public static JsonElement GetNestedProperty( JsonObject root, String path ) {
        // navigate to the nested property
        JsonElement je = root;
        String[] pathParts = path.split( "[.]" );
        String part = null;
        int index = 0;
        int maxIndex = pathParts.length - 1;

        while( index < maxIndex ) {
            part = pathParts[ index ];

            JsonObject joParent = ( JsonObject ) je; // there is more to find
            JsonElement jeChild = joParent.get( part );

            ++index;

            je = jeChild;
        }

        return je;
    }

    /**
     * Create an entity as specified, generate its config, and persist to disk.
     *
     * @param name
     * @param type
     * @param node
     * @param parent
     */
    public static void CreateEntity( String name, String type, String node, String parent ) {
        String config = "";
        ModelEntity model = new ModelEntity( name, type, node, parent, config );
        CreateEntity( model );
    }

    /**
     * Create an entity in the persistence layer using the model.
     * Create config object from data model, convert to a string, set back to model and persist.
     * The effect is that any undefined fields will still be present (with value of null) in the persistence layer.
     *
     * @param model
     */
    public static void CreateEntity( ModelEntity model ) {
        Node node = Node.NodeInstance();
        Entity entity = node.getEntityFactory().create( node.getObjectMap(), model );
        EntityConfig entityConfig = entity.createConfig();
        model.config = Entity.SerializeConfig( entityConfig );
        node.getPersistence().persistEntity( model );
    }

    /**
     * Create entities in the persistence layer, represented in the file (see file format).
     * TODO document the file format
     *
     * @param file
     */
    public static void LoadEntities( String file ) {
        try {
            String jsonEntities = FileUtil.readFile( file );
            ImportEntities( jsonEntities );
        }
        catch( Exception e ) {
            logger.error( e.getStackTrace() );
            System.exit( -1 );
        }
    }

    /**
     * Import Entities to the system from serialized form as Json.
     *
     * @param jsonEntities
     * @throws Exception
     */
    public static void ImportEntities( String jsonEntities ) throws Exception {
        Gson gson = new Gson();

        try {
            Type listType = new TypeToken< List< ModelEntity > >() {
            }.getType();
            List< ModelEntity > entities = gson.fromJson( jsonEntities, listType );

            for( ModelEntity modelEntity : entities ) {
                logger.info( "Persisting Entity of type: " + modelEntity.type + ", that is hosted at Node: " + modelEntity.node );
                CreateEntity( modelEntity );
            }
        }
        catch( Exception e ) {
            throw( e );
        }
    }

    /**
     * Load Data objects from file into the system.
     * @param file
     */
    public static void LoadData( String file ) {
        try {
            String jsonData = FileUtil.readFile( file );
            ImportData( jsonData );
        }
        catch( Exception e ) {
            logger.error( e.getStackTrace() );
            System.exit( -1 );
        }
    }

    /**
     * Import Data objects to the system from serialized form as Json.
     *
     * @param jsonData
     * @throws Exception
     */
    public static void ImportData( String jsonData ) throws Exception {
        Gson gson = new Gson();
        try {
            Type listType = new TypeToken< List< ModelData > >() {
            }.getType();

            List< ModelData > modelDatas = gson.fromJson( jsonData, listType );
            for( ModelData modelData : modelDatas ) {
                Framework.SetData( modelData );
            }
        }
        catch( Exception e ) {
            throw( e );
        }
    }

    /**
     * Load data references from file, which allows data to be mapped from one entity to another (input and outputs).
     * @param file
     */
    public static void LoadDataReferences( String file ) {
        Gson gson = new Gson();
        try {
            String jsonEntity = FileUtil.readFile( file );

            Type listType = new TypeToken< List< ModelDataReference > >() {
            }.getType();

            List< ModelDataReference > references = gson.fromJson( jsonEntity, listType );
            for( ModelDataReference modelDataReference : references ) {
                logger.info( "Persisting data input reference for data: " + modelDataReference.dataKey + " with input data keys: " + modelDataReference.refKeys );
                Framework.SetDataReference( modelDataReference.dataKey, modelDataReference.refKeys );
            }
        }
        catch( Exception e ) {
            logger.error( e.getStackTrace() );
            System.exit( -1 );
        }
    }

    public static void LoadConfigs( String file ) {
        Gson gson = new Gson();
        try {
            String jsonEntity = FileUtil.readFile( file );

            Type listType = new TypeToken< List< ModelEntityConfigPath > >() {
            }.getType();
            List< ModelEntityConfigPath > modelConfigs = gson.fromJson( jsonEntity, listType );

            for( ModelEntityConfigPath modelConfig : modelConfigs ) {

                logger.info( "Persisting entity: " + modelConfig._entityName + " config path: " + modelConfig._configPath + " value: " + modelConfig._configValue );

                Framework.SetConfig( modelConfig._entityName, modelConfig._configPath, modelConfig._configValue );
            }
        }
        catch( Exception e ) {
            logger.error( e.getStackTrace() );
            System.exit( -1 );
        }
    }

    protected static String GetEntityDataSubtree( String entityName ) {
        Gson gson = new Gson();
        Collection< ModelData > modelData = new ArrayList<>();

        GetEntityDataSubtree( entityName, modelData );

        String export = gson.toJson( modelData );
        return export;
    }

    /**
     * Get all the Data models for all entities in the subtree, and put in a flat collection.
     *
     * @param entityName the parent of the subtree.
     * @param modelDatas the flat collection that will contain the data models.
     */
    protected static void GetEntityDataSubtree( String entityName, Collection< ModelData > modelDatas ) {
        Node node = Node.NodeInstance();
        AddEntityData( entityName, modelDatas );

        Collection< String > childNames = node.getPersistence().getChildEntities( entityName );
        for( String childName : childNames ) {
            GetEntityDataSubtree( childName, modelDatas );
        }
    }

    protected static void AddEntityData( String entityName, Collection< ModelData > modelDatas ) {

        Node node = Node.NodeInstance();

        ModelEntity modelEntity = node.getPersistence().fetchEntity( entityName );

        Entity entity = node.getEntityFactory().create( node.getObjectMap(), modelEntity );
        entity._config = entity.createConfig();

        Collection< String > attributes = new ArrayList<>();
        DataFlags dataFlags = new DataFlags();
        entity.getOutputAttributes( attributes, dataFlags );

        for( String attribute : attributes ) {
            String outputKey = entity.getKey( attribute );
            ModelData modelData = node.getPersistence().fetchData( outputKey );

            if( modelData != null ) {
                modelDatas.add( modelData );
            }
        }
    }

    protected static String GetEntitySubtree( String entityName ) {
        Gson gson = new Gson();
        Collection< ModelEntity > modelEntities = new ArrayList<>();
        AddEntitySubtree( entityName, modelEntities );
        String export = gson.toJson( modelEntities );
        return export;
    }

    /**
     * Flatten subtree of a given entity, referenced by name, into a collection of entity models.
     * Recursive method.
     *
     * @param entityName
     * @param modelEntities
     */
    protected static void AddEntitySubtree( String entityName, Collection< ModelEntity > modelEntities ) {
        // traverse tree depth first via recursion, building the string representation
        Persistence persistence = Node.NodeInstance().getPersistence();
        ModelEntity modelEntity = persistence.fetchEntity( entityName );
        modelEntities.add( modelEntity );

        Collection< String > childNames = persistence.getChildEntities( entityName );
        for( String childName : childNames ) {
            AddEntitySubtree( childName, modelEntities );
        }
    }

    /**
     * Export a subtree of entities and data, in the form of a serialised representation that allows full re-import,
     * to view or resume.
     *
     * @param entityName the parent of the subtree
     * @return serialised form of subtree
     */
    public static String ExportSubtree( String entityName, String type ) {
        String entitiesExport = null;

        if( type.equalsIgnoreCase( HttpExportHandler.TYPE_ENTITY ) ) {
            entitiesExport = GetEntitySubtree( entityName );
        } else if( type.equalsIgnoreCase( HttpExportHandler.TYPE_DATA ) ) {
            entitiesExport = GetEntityDataSubtree( entityName );
        }

        return entitiesExport;
    }

    /**
     * Import a subtree of entities and data.
     *
     * @param jsonEntities
     * @param jsonData
     * @return
     */
    public static boolean ImportSubtree( String jsonEntities, String jsonData ) {
        try {
            Framework.ImportEntities( jsonEntities );
            Framework.ImportData( jsonData );
            return true;
        }
        catch( Exception e ) {
            logger.error( e.getStackTrace() );
            return false;
        }
    }


}