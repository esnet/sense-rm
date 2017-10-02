/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.sense.rm.driver.nsi.db;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 *
 * @author hacksaw
 */
public class JsonExclusionStrategy implements ExclusionStrategy {
    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return "self".contentEquals(f.getName());
    }
}