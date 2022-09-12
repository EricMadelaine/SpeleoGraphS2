/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cds06.speleograph;

/**
 *
 * @author Léa
 */
class Caves {
    
    public String id;
    public String name;
    public String latitude;
    public String longitude;
    
    public Caves(String id, String name, String latitude, String longitude){
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public String getid() {
     return this.name;
    }
    public void setid(String id) {
     this.id = id;
    }
    public void setname(String name) {
     this.name = name;
    }
    public void setlatitude(String latitude) {
     this.latitude = latitude;
    }
    public void setlongitude(String longitude) {
     this.longitude = longitude;
    }
}
