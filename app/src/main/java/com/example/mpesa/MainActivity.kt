package com.example.mpesa

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.androidstudy.daraja.Daraja
import com.androidstudy.daraja.callback.DarajaResult
import com.androidstudy.daraja.util.Environment
import com.example.mpesa.databinding.ActivityMainBinding
import com.example.mpesa.util.AppUtils
import com.example.mpesa.util.Config

class MainActivity : AppCompatActivity() {
    private lateinit var progressDialog: ProgressDialogFragment
    private lateinit var daraja: Daraja
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "Payment"

        // initialize daraja
        daraja = getDaraja()

        accessToken()
    }

    private fun getDaraja(): Daraja {
        return Daraja.builder(Config.CONSUMER_KEY, Config.CONSUMER_SECRET)
            .setBusinessShortCode(Config.BUSINESS_SHORTCODE)
            .setPassKey(AppUtils.passKey)
            .setTransactionType(Config.ACCOUNT_TYPE)
            .setCallbackUrl(Config.CALLBACK_URL)
            .setEnvironment(Environment.SANDBOX)
            .build()
    }

    private fun pay() {
        val phoneNumber = binding.etPhoneNumber.text.toString()
        val amountString = binding.etAmount.text.toString()

        if (phoneNumber.isBlank() || amountString.isBlank()) {
            toast("You have left some fields blank")
            return
        }

        val amount = amountString.toInt()
        initiatePayment(phoneNumber, amount)
    }

    private fun initiatePayment(phoneNumber: String, amount: Int) {
        val token = AppUtils.getAccessToken(baseContext)
        if (token == null) {
            accessToken()
            toast("Your access token was refreshed. Retry again.")
        } else {
            // initiate payment
            showProgressDialog()
            daraja.initiatePayment(token, phoneNumber, amount.toString(), AppUtils.generateUUID(), "Payment") { darajaResult ->
                dismissProgressDialog()
                when (darajaResult) {
                    is DarajaResult.Success -> {
                        val result = darajaResult.value
                        toast(result.ResponseDescription)
                    }
                    is DarajaResult.Failure -> {
                        val exception = darajaResult.darajaException
                        if (darajaResult.isNetworkError) {
                            toast(exception?.message ?: "Network error!")
                        } else {
                            toast(exception?.message ?: "Payment failed!")
                        }
                    }
                }
            }
        }
    }

    private fun accessToken() {
        // get access token
        showProgressDialog()
        daraja.getAccessToken { darajaResult ->
            dismissProgressDialog()
            when (darajaResult) {
                is DarajaResult.Success -> {
                    val accessToken = darajaResult.value
                    AppUtils.saveAccessToken(baseContext, accessToken.access_token)
                    binding.bPay.setOnClickListener { pay() }
                }
                is DarajaResult.Failure -> {
                    val darajaException = darajaResult.darajaException
                    toast(darajaException?.message ?: "An error occurred!")
                    binding.bPay.setOnClickListener { accessToken() }
                }
            }
        }
    }

    private fun toast(text: String) = Toast.makeText(baseContext, text, Toast.LENGTH_LONG).show()

    private fun showProgressDialog(title: String = "This will only take a sec", message: String = "Loading") {
        progressDialog = ProgressDialogFragment.newInstance(title, message)
        progressDialog.isCancelable = false
        progressDialog.show(supportFragmentManager, "progress")
    }

    private fun dismissProgressDialog() {
        progressDialog.dismiss()
    }
}