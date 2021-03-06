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

package io.agi.framework.coordination.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.agi.core.orm.AbstractPair;
import io.agi.framework.persistence.PersistenceUtil;
import io.agi.framework.references.DataRefMap;
import io.agi.framework.references.DataRef;
import io.agi.framework.Framework;
import io.agi.framework.Node;
import io.agi.framework.persistence.models.ModelData;
import io.agi.framework.references.DataRefUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by dave on 17/03/16.
 */
public class HttpDataHandler implements HttpHandler {

    protected static final Logger _logger = LogManager.getLogger();

    public static final String CONTEXT = "/data";

    public static final String PARAMETER_NAME = "name";
    public static final String PARAMETER_MODEL = "model";
    public static final String PARAMETER_FILTER = "filter";

    public HttpDataHandler() {}

    @Override
    public void handle( HttpExchange t ) throws IOException {
        int status = 400;
        String response = "";

        try {
            Node n = Node.NodeInstance();
            DataRefMap dataRefMap = n.getDataRefMap();

            String query = t.getRequestURI().getQuery();
            _logger.info( "Request: " + HttpDataHandler.CONTEXT + " " + query );

            String method = t.getRequestMethod();

            ArrayList< AbstractPair< String, String > > parameters = HttpUtil.GetDuplicateQueryParams( query );

            if( method.equalsIgnoreCase( "GET" ) ) {

                // fetch any existing data mentioned..
                Collection< ModelData > results = new ArrayList<>();

                for( AbstractPair< String, String > ap : parameters ) {
                    String key = ap._first;
                    String value = ap._second;
                    if( key.equalsIgnoreCase( PARAMETER_NAME ) ) {
                        DataRef dr = dataRefMap.getData( value );
                        ModelData md = new ModelData();
                        boolean ok = md.serialize( dr );
//                        ModelData m = n.getModelData( value, new DataRef.DenseDataRefResolver() );
                        if( ok ) {
                            results.add( md ); // a complete data (specifically fetched)
                        }
                    }
                    else if( key.equalsIgnoreCase( PARAMETER_FILTER ) ) {
                        DataRefMap map = n.getDataRefMap();
                        Collection< ModelData > c = map.getDataMeta( value );
                        //Collection< ModelData > c = n.getPersistence().getDataMeta( value );
                        results.addAll( c );
                    }
                }

                // if no data specified, get all data names.
                if( results.isEmpty() ) {

                    Collection< String > names = dataRefMap.getDataKeys();

                    for( String name : names ) {
                        ModelData m = new ModelData();
                        m.name = name;
                        results.add( m );
                    }
                }

                // build the response
//                boolean first = true;
//
//                response += "[ ";
//                System.err.println( "Handling /data call 1." );
//
//                for( ModelData m : results ) {
//                    if( first ) {
//                        first = false;
//                    } else {
//                        response += ",";
//                    }
//
//                    response += "{ ";
//
//                    response += " \"name\": \"" + m.name + "\"" + ",";
//                    response += " \"refKeys\": \"" + m.refKeys + "\"" + ",";
//                    response += " \"sizes\": " + m.sizes + ",";
//                    response += " \"elements\": " + m.elements;
//
//                    response += " }";
//                }
//                System.err.println( "Handling /data call 2." );
//
//                response += " ]";
                response = ModelData.ModelDatasToJsonString( results ); // high efficiency

                status = 200;
            }
            else if( method.equalsIgnoreCase( "POST" ) ) {

                for( AbstractPair< String, String > ap : parameters ) {
                    String key = ap._first;
                    String value = ap._second;

                    if( key.equalsIgnoreCase( PARAMETER_MODEL ) ) {
                        String jsonData = value;
                        DataRefUtil.ImportData( jsonData );
                    }
                }

                response = "Persisted data OK.";

                status = 200;
            }

        }
        catch( Exception e ) {
            _logger.error( "Unable to handle data call.");
            _logger.error( e.toString(), e );
        }

        HttpUtil.SendResponse( t, status, response );
    }
}
