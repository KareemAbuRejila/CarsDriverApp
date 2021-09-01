package com.codeshot.cars.Starting

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.codeshot.cars.Common.Common
import com.codeshot.cars.MapsActivity
import com.codeshot.cars.Models.User
import com.codeshot.cars.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import dmax.dialog.SpotsDialog
import uk.co.chrisjenx.calligraphy.CalligraphyConfig

class LoginActivity : AppCompatActivity() {
    //View

    private var edtEmail: EditText? = null
    private var edtPass: EditText? = null
    private var tvForgotPassword: TextView? = null
    private var tvSignUp: TextView? = null
    private var btnLogin: Button? = null
    private var progressBar: ProgressBar? = null
    private var waitingDialog: AlertDialog? = null

    //FireBase
    private var mAuth: FirebaseAuth? = null
    private var driversRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Before setContentView
        CalligraphyConfig.initDefault(CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/SFProText-Regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build())
        setContentView(R.layout.activity_login)
        initializations()
        tvForgotPassword!!.setOnClickListener { sendToForGotPasswordActivity() }
        tvSignUp!!.setOnClickListener { sendToRegisterActivity() }
        btnLogin!!.setOnClickListener { login() }
    }

    private fun login() {
        val email: String
        val password: String
        email = edtEmail!!.text.toString()
        password = edtPass!!.text.toString()
        if (password.length < 8) {
            edtPass!!.error = "Password too short"
            return
        }
        waitingDialog!!.show()
        mAuth!!.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                waitingDialog!!.dismiss()
                FirebaseDatabase.getInstance().getReference(Common.drivers_tbl)
                        .child(FirebaseAuth.getInstance().currentUser!!.uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                Common.currentUser = dataSnapshot.getValue(User::class.java)
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.e("ERROR ADD USER",databaseError.message)
                            }
                        })
                sendToMapActivity()

            } else {
                Toast.makeText(this@LoginActivity, task.exception!!.message, Toast.LENGTH_LONG).show()
                waitingDialog!!.dismiss()
            }
        }
    }

    private fun initializations() {
        //View
        edtEmail = findViewById<View>(R.id.edtEmailLog) as EditText
        edtPass = findViewById<View>(R.id.edtPassword) as EditText
        tvForgotPassword = findViewById<View>(R.id.tvForgotPasswordLog) as TextView
        tvSignUp = findViewById<View>(R.id.tvSignUpLog) as TextView
        edtEmail = findViewById<View>(R.id.edtEmailLog) as EditText

        btnLogin = findViewById<View>(R.id.btnLogin) as Button
        progressBar = findViewById<View>(R.id.progress_circularLog) as ProgressBar
        waitingDialog = SpotsDialog.Builder().setContext(this).build()
        waitingDialog!!.setTitle("Waiting")
        waitingDialog!!.setMessage("waiting to log in Cars App")
        //FireBase
        mAuth = FirebaseAuth.getInstance()
        driversRef = FirebaseDatabase.getInstance().reference.child(Common.drivers_tbl)
    }

    private fun sendToRegisterActivity() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    private fun sendToMapActivity() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun sendToForGotPasswordActivity() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }
}
