package amai.org.pushnotificationsender

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationsActivity : AppCompatActivity() {

    private lateinit var requestQueue: RequestQueue
    private lateinit var editText: EditText

    private var latestMessageId = UUID.randomUUID()

    private val API_URL = "https://us-central1-starlit-brand-95018.cloudfunctions.net/sendPush"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_notifications)
        requestQueue = Volley.newRequestQueue(this)
        editText = findViewById(R.id.editText)
    }

    fun sendNotificationButtonOnClick(view: View) {
        if (TextUtils.isEmpty(editText.text)) {
            Toast.makeText(this, R.string.error_missing_message, Toast.LENGTH_SHORT).show()
            return
        }

        val checkedRadioButtonId = findViewById<RadioGroup>(R.id.radio_group).checkedRadioButtonId
        val category = findViewById<RadioButton>(checkedRadioButtonId).text

        AlertDialog.Builder(this)
                .setTitle(getString(R.string.message_in_tag, category))
                .setMessage(getString(R.string.message_send_to_count, category))
                .setPositiveButton(R.string.send) { _, _ ->
                    sendNotification()
                }
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show()
    }

    private fun sendNotification() {
        val notificationId = UUID.randomUUID()
        Log.v("NotificationsActivity", "getting token to send notification $notificationId")

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            finish()
            return
        }
        user.getIdToken(true).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                sendNotification(task.result.token ?: "", notificationId)
            } else {
                Toast.makeText(this, "failed to obtain user id token", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendNotification(token: String, id: UUID) {
        Log.v("NotificationsActivity", "sending notification $id")

        // On some occasions this method may be called multiple times for the same message by firebase.
        // See https://github.com/firebase/quickstart-android/issues/80
        // To workaround this, ignore messages with identical IDs.
        if (latestMessageId == id) {
            Log.w("NotificationsActivity", "Ignoring duplicate message $id")
            return
        }
        latestMessageId = id

        val checkedRadioButtonId = findViewById<RadioGroup>(R.id.radio_group).checkedRadioButtonId
        val category = findViewById<View>(checkedRadioButtonId).tag.toString()
        val json = Gson().toJson(Notification(category, null, editText.text.toString(), id.toString()))

        val request = object : StringRequest(Request.Method.POST, API_URL, Response.Listener<String> {
            Toast.makeText(this@NotificationsActivity, R.string.send_success, Toast.LENGTH_SHORT).show()
        }, Response.ErrorListener { error ->
            Toast.makeText(this@NotificationsActivity, R.string.send_failed, Toast.LENGTH_SHORT).show()
            Log.e("NotificationsActivity", "failed to send push with error ", error)
        }) {
            override fun getBody(): ByteArray {
                return json.toByteArray()
            }

            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Authorization", "Bearer " + token)
                headers.put("Content-Type", "application/json")
                return headers
            }
        }

        request.retryPolicy = DefaultRetryPolicy(
                TimeUnit.SECONDS.toMillis(60).toInt(),
                0,  // Disabling retry, since it may cause duplicated push notifications to be sent.
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        request.setShouldCache(false)

        Toast.makeText(this, R.string.send_in_progress, Toast.LENGTH_SHORT).show()
        requestQueue.add(request)
    }

    private class Notification(val topic: String, val title: String?, val body: String, val id: String)
}