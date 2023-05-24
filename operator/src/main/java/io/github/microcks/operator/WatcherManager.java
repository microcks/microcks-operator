/*
 * Licensed to Laurent Broudoux (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.microcks.operator;

import io.fabric8.kubernetes.client.Watcher;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple manager for Watcher that allows registering / unregistering them so that
 * controllers do not create multiple watchers for the same resources...
 * @author laurent
 */
public class WatcherManager {

   /** Singleton's internal instance. */
   private static final WatcherManager singleton = new WatcherManager();

   /** Manager is baked by a concurrent hash map where watchers are stored according a key. */
   private final ConcurrentHashMap<WatcherKey, Watcher> watchers = new ConcurrentHashMap<>();

   private WatcherManager() {
   }

   /**
    * Retrieve the current and unique instance of Watcher manager.
    * @return A WatcherManager instance
    */
   public static WatcherManager getInstance() {
      return singleton;
   }

   /**
    * Returns true if a watcher has already been registered with this key.
    * @param key The registration key for watcher
    * @return True if already present, false otherwise.
    */
   public boolean hasWatcher(WatcherKey key) {
      return watchers.containsKey(key);
   }

   /**
    * Returns registered watcher or null if none.
    * @param key The registration key for watcher
    * @return Registered watcher or null
    */
   public Watcher getWatcher(WatcherKey key) {
      return watchers.get(key);
   }

   /**
    * Register the given matcher using its key.
    * @param key The registration key for watcher
    * @param watcher The watcher to register
    */
   public void registerWatcher(WatcherKey key, Watcher watcher) {
      watchers.put(key, watcher);
   }

   /**
    * Unregister an existing watcher if any.
    * @param key The registration key for watcher
    */
   public void unregisterWatcher(WatcherKey key) {
      watchers.remove(key);
   }
}
