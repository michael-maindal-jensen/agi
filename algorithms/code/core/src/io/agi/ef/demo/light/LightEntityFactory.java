package io.agi.ef.demo.light;

import io.agi.core.orm.ObjectMap;
import io.agi.ef.Entity;
import io.agi.ef.EntityFactory;
import io.agi.ef.Node;
import io.agi.ef.entities.CommonEntityFactory;

/**
 * Created by dave on 20/02/16.
 */
public class LightEntityFactory extends CommonEntityFactory {

    public LightEntityFactory() {

    }

    public Entity create( ObjectMap om, String entityName, String entityType ) {

        Entity e = super.create( om, entityName, entityType );
        if( e != null ) {
            return e;
        }

        if( entityType.equals( LightSourceEntity.ENTITY_TYPE ) ) {
            return new LightSourceEntity( entityName, om, LightSourceEntity.ENTITY_TYPE, _n );
        }
        if( entityType.equals( LightControlEntity.ENTITY_TYPE ) ) {
            return new LightControlEntity( entityName, om, LightControlEntity.ENTITY_TYPE, _n );
        }
        return null;
    }

}
