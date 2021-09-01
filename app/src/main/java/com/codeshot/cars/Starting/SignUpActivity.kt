package com.codeshot.cars.Starting

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.codeshot.cars.Common.Common
import com.codeshot.cars.Models.User
import com.codeshot.cars.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import uk.co.chrisjenx.calligraphy.CalligraphyConfig

class SignUpActivity : AppCompatActivity() {
    //View
    private var edtUserName: EditText? = null
    private var edtEmail: EditText? = null
    private var edtPass: EditText? = null
    private var edtRePass: EditText? = null
    private var edtUserPhone: EditText? = null
    private var switch_gender: SwitchMaterial? = null
    private var gender: String? = null
    private var btnRegister: Button? = null
    private var coordinatorLayout: CoordinatorLayout? = null
    private var progressBar: ProgressBar? = null
    //Firebase
    private var mAuth: FirebaseAuth? = null
    private var driversRef: DatabaseReference? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Before setContentView
        CalligraphyConfig.initDefault(CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/SFProText-Regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build())
        setContentView(R.layout.activity_signup)
        initializations()

        switch_gender!!.setOnCheckedChangeListener { buttonView, isChecked ->
            gender = if (isChecked) {
                "male"
            } else "female"
        }
        btnRegister!!.setOnClickListener { v -> Register(v) }
    }

    private fun Register(v: View) {
        val userName: String
        val email: String
        val password: String
        val rePassword: String
        val phoneNumber: String
        userName = edtUserName!!.text.toString()
        email = edtEmail!!.text.toString()
        password = edtPass!!.text.toString()
        rePassword = edtRePass!!.text.toString()
        phoneNumber = edtUserPhone!!.text.toString()
        if (!switch_gender!!.isChecked) {
            Snackbar.make(v, "Select Your Gender please ...", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (password.length < 8) {
            edtPass!!.error = "Password too short"
            return
        }
        if (password != rePassword) {
            edtRePass!!.error = "passwords not sames"
            return
        }
        //Register
        progressBar!!.visibility = View.VISIBLE
        if (gender != null) {
            mAuth!!.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userKey = mAuth!!.currentUser!!.uid
                    val user = User(userName, email, phoneNumber, gender)
                    driversRef!!.child(userKey).setValue(user).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Snackbar.make(v, "Successful Register", Snackbar.LENGTH_SHORT).show()
                            progressBar!!.visibility = View.GONE
                            sendToLoginActivity()
                        } else {
                            progressBar!!.visibility = View.GONE
                            Snackbar.make(v, task.exception!!.message!!, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    progressBar!!.visibility = View.GONE
                    Snackbar.make(v, task.exception!!.message!!, Snackbar.LENGTH_SHORT).show()
                }
            }
        } else Snackbar.make(v, "Gender Is null", Snackbar.LENGTH_SHORT).show()
    }

    private fun sendToLoginActivity() {
        val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun initializations() { //View
        edtUserName = findViewById<View>(R.id.edtUserNameReg) as EditText
        edtEmail = findViewById<View>(R.id.edtEmailReg) as EditText
        edtPass = findViewById<View>(R.id.edtPasswordReg) as EditText
        edtRePass = findViewById<View>(R.id.edtPasswordReg) as EditText
        edtUserPhone = findViewById<View>(R.id.edtUserPhoneReg) as EditText
        switch_gender = findViewById<View>(R.id.switch_gender) as SwitchMaterial
        btnRegister = findViewById<View>(R.id.btnSignUp) as Button
        coordinatorLayout = findViewById<View>(R.id.signUpLayout) as CoordinatorLayout
        progressBar = findViewById<View>(R.id.progress_circularReg) as ProgressBar
        //FireBase
        mAuth = FirebaseAuth.getInstance()
        driversRef = FirebaseDatabase.getInstance().reference.child(Common.drivers_tbl)
    }
}