package amai.org.pushnotificationsender


import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient

    private val SIGN_IN_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("458203975509-qghf2og9qjhlt8hmd8pv1d2ttra5pk8h.apps.googleusercontent.com")
                .build());

        findViewById<View>(R.id.sign_in_button).setOnClickListener({
            startActivityForResult(mGoogleSignInClient.getSignInIntent(), SIGN_IN_REQUEST_CODE)
        })
    }

    override fun onStart() {
        super.onStart()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            startActivity(Intent(this, NotificationsActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SIGN_IN_REQUEST_CODE) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                signInToFirebase(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "fail! " + e.statusCode, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInToFirebase(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener({ task ->
            if (task.isSuccessful) {
                startActivity(Intent(this, NotificationsActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                Toast.makeText(this, "failed to sign in to firebase", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
