package com.gcw.sapienza.places.models.manager;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;

/**
 * Base class to handle queries to Parse.com
 */
public abstract class ModelManager<T extends ParseObject> {


    /**
     *
     * @param identifier identifier of the object
     * @return the Promise associated with the query
     */
    public Promise<T> withIdentifier(String identifier){
        ParseQuery<T> query  = this.instantiateEmptyQuery();
        query.whereEqualTo("objectId", identifier);
       return new Promise<>(query);
    }

    /**
     * All subclasses must implement this method returning the right class:
     *
     *          return Flag.class
     *
     * @return an empty query
     */
    protected abstract Class<T> parseClass();


    /**
     *
     * @return a query instantiated with the correct class
     */
    protected   ParseQuery<T> instantiateEmptyQuery(){
         String className = this.parseClass().getAnnotation(ParseClassName.class).value();
        return ParseQuery.getQuery(className);

    }



}
