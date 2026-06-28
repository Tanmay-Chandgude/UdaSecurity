module security.service {

    requires java.desktop;
    requires java.prefs;

    requires image.service;

    requires com.google.common;
    requires com.google.gson;
    requires com.miglayout.core;
    requires com.miglayout.swing;
    requires org.slf4j;

    opens com.udacity.catpoint.data to com.google.gson;
}