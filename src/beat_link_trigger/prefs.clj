(ns beat-link-trigger.prefs
  "Functions for managing application preferences"
  (:require [clojure.edn :as edn]
            [fipp.edn :as fipp]
            [beat-link-trigger.about :as about])
  (:import java.util.prefs.Preferences))

(defonce ^{:private true
           :doc "The custom readers needed to read in our preferences"}
  prefs-readers
  (atom {}))

(defn add-reader
  "Add a custom reader function that can be used to parse an
  application object out of the saved preferences."
  [tag f]
  (swap! prefs-readers assoc tag f))

(defn- prefs-node
  "Return the node at which we store our preferences."
  []
  (.node (Preferences/userRoot) "org.deepsymmetry.beat_link_trigger"))

(defn get-preferences
  "Returns the current values of the user preferences, creating them
  if they did not exist."
  []
  (if-let [existing (.get (prefs-node) "prefs" nil)]
    (edn/read-string {:readers @prefs-readers} existing)
    {:beat-link-trigger-version (about/get-version)}))

(defn put-preferences
  "Updates the user preferences to reflect the map supplied."
  [m]
  (let [prefs (prefs-node)]
    (.put prefs "prefs" (prn-str m))
    (.flush prefs)))

(defn save-to-file
  "Saves the preferences to a text file."
  [file]
  (spit file (with-out-str (fipp/pprint (get-preferences)))))

(defn valid-file?
  "Checks whether the specified file seems to be a valid save file. If
  so, returns it; otherwiser returns nil."
  [file]
  (try
    (with-open [in (java.io.PushbackReader. (clojure.java.io/reader file))]
      (let [m (edn/read {:readers @prefs-readers} in)]
        (when (some? (:beat-link-trigger-version m))
          m)))
    (catch Exception e
      nil)))

(defn load-from-file
  "Read the preferences from a text file."
  [file]
  (if (valid-file? file)
    (with-open [in (java.io.PushbackReader. (clojure.java.io/reader file))]
      (let [m (edn/read {:readers @prefs-readers} in)]
        (put-preferences m)
        m))
    (throw (IllegalArgumentException. (str "Unreadable file: " file)))))