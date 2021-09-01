package com.codeshot.cars.Models

import com.firebase.geofire.GeoFire
import java.io.Serializable

class User : Serializable {
    var userName: String? = null
    var email: String? = null
    var phoneNumber: String? = null
    var gender: String? = null
    var geoFire: GeoFire? = null


    constructor() {}
    constructor(userName: String?, email: String?, phoneNumber: String?, gender: String?) {
        this.userName = userName
        this.email = email
        this.phoneNumber = phoneNumber
        this.gender = gender
    }

}