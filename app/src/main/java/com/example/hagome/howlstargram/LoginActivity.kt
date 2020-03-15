package com.example.hagome.howlstargram

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.*
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*

class LoginActivity : AppCompatActivity() {
    var auth: FirebaseAuth? = null

    var googleSignInClient: GoogleSignInClient? = null

    var GOOGLE_LOGIN_CODE = 9001
    var callbackManager: CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance();
        email_login_button.setOnClickListener {
            createAndLoginEmail()
        }
        google_sign_in_button.setOnClickListener {
            googleLogin()
        }

        facebook_login_button.setOnClickListener {
            facebookLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        callbackManager = CallbackManager.Factory.create()

    }

    fun createAndLoginEmail() { //이메일 회원가입 및 로그인

        if(email_edittext.text.toString().isNullOrEmpty()){
            Toast.makeText(this, "아이디를 입력하세요.", Toast.LENGTH_LONG).show()
        } else if(!email_edittext.text.toString().isNullOrEmpty() && password_edittext.text.toString().isNullOrEmpty()){
            Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_LONG).show()
        } else {
            //주어진 전자 메일 주소와 암호로 새 사용자 계정을 만들려고 시도
            auth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
                    ?.addOnCompleteListener { task ->

                        if (task.isSuccessful) {    //새로운 계정
                            moveMainPage(auth?.currentUser)
                            Toast.makeText(this, "아이디 생성이 완료되었습니다.", Toast.LENGTH_LONG).show()
                        } else if (task.exception?.message.isNullOrEmpty()) {   //예외 오류
                            Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                        } else {    //기존 계정
                            signinEmail()
                        }
                    }
        }
    }

    fun signinEmail() {
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        moveMainPage(auth?.currentUser)
                        Toast.makeText(this, "로그인이 성공했습니다.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                    }
                }
    }

    fun moveMainPage(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))   //페이지 전환
            finish()    //다음 창으로 전환 후 현재 페이지 접근 불가
        }
    }

    fun googleLogin() {

        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        var credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener {

            task ->
            if(task.isSuccessful){
                moveMainPage(auth?.currentUser)
            }
        }

    }

    fun facebookLogin() {
        LoginManager
                .getInstance()
                .logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))
        LoginManager
                .getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult?) {
                        handleFacebookAccessToken(result?.accessToken)
                    }

                    override fun onCancel() {

                    }

                    override fun onError(error: FacebookException?) {

                    }
                })
    }

    fun handleFacebookAccessToken(token: AccessToken?) {
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                moveMainPage(auth?.currentUser)
            }
        }
    }

    // 로그인 상태 유지
    override fun onResume() {
        super.onResume()
        moveMainPage(auth?.currentUser)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_LOGIN_CODE) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                var account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            }
        }
    }
}