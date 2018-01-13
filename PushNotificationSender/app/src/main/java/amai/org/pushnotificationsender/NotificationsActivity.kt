package amai.org.pushnotificationsender

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson

class NotificationsActivity : AppCompatActivity() {

    private lateinit var requestQueue: RequestQueue
    private lateinit var editText: EditText
    private lateinit var remainingChars: TextView

    private val API_URL = "https://us-central1-starlit-brand-95018.cloudfunctions.net/sendPush"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_notifications)
        requestQueue = Volley.newRequestQueue(this)

        editText = findViewById(R.id.editText)
        remainingChars = findViewById(R.id.remainingCharsCount)

        editText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                remainingChars.text = String.format("%d/1900", editText.length())
            }
        })
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
                .setPositiveButton(R.string.send, { _, _ ->
                    sendNotification()
                })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show()
    }

    private fun sendNotification() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            finish()
            return
        }
        user.getIdToken(true).addOnCompleteListener({task ->
            if (task.isSuccessful) {
                sendNotification(task.result.token ?: "")
            } else {
                Toast.makeText(this, "failed to obtain user id token", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendNotification(token: String) {
        val checkedRadioButtonId = findViewById<RadioGroup>(R.id.radio_group).checkedRadioButtonId
        val category = findViewById<View>(checkedRadioButtonId).tag.toString()
        val json = Gson().toJson(Notification(category, null, editText.text.toString()))

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

        Toast.makeText(this, R.string.send_in_progress, Toast.LENGTH_SHORT).show()
        requestQueue.add(request)
    }

    private class Notification(val topic: String, val title: String?, val body: String)
}