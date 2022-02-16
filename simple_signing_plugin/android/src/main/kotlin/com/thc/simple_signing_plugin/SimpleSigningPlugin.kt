package com.thc.simple_signing_plugin

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.widget.Toast
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.security.auth.x500.X500Principal
import kotlin.properties.Delegates

/** SimpleSigningPlugin */
class SimpleSigningPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
  private lateinit var channel : MethodChannel
  private lateinit var context: Context
  private lateinit var activity: Activity
  private lateinit var keyguardManager: KeyguardManager
  private lateinit var keyPair: KeyPair
  private lateinit var signatureResult: String
  private var isDeviceSecure by Delegates.notNull<Boolean>()
  private var dataToSign: String = ""
  private var dataSignature: String = ""
  private lateinit var pendingResult: Result


  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "simple_signing_plugin")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    checkIfDeviceSecure()

    if (!checkKeyExists()) {
      generateKey()
    }
  }

  private fun checkIfDeviceSecure() : Boolean{
    return if (!keyguardManager.isDeviceSecure) {
      Toast.makeText(context, "Secure lock screen hasn't set up.", Toast.LENGTH_LONG).show()
      isDeviceSecure = false
      false
    }else{
      isDeviceSecure = true
      true
    }
  }

  fun registerWith(registrar: PluginRegistry.Registrar) {
    activity = registrar.activity()
    val channel = MethodChannel(registrar.messenger(), "simple_signing_plugin")
    channel.setMethodCallHandler(SimpleSigningPlugin())
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "signData"){
      this.pendingResult = result
      val data = call.argument<String>("data")
      if (data != null) {
        dataToSign = data
        val intent: Intent? = keyguardManager.createConfirmDeviceCredentialIntent("Keystore Sign And Verify",
          "In order to sign the data you need to confirm your identity. Please enter your pin/pattern or scan your fingerprint")
        if (intent != null) {
          activity.startActivityForResult(intent, REQUEST_CODE_FOR_CREDENTIALS)
        }
      }else{
        result.error("UNAVAILABLE", "Data cannot be null!", null)
      }
    }else if (call.method == "verifyData"){
      val data = call.argument<String>("data")
      if(data != null){
        val isValid = verifyData(data)
        if(isValid){
          result.success(true)
        }else{
          result.success(false)
        }
      }else{
        result.error("UNAVAILABLE", "Key cannot be null!", null)
      }
    }else if(call.method == "checkIfDeviceSecure"){
      val getResult = checkIfDeviceSecure()
      if (getResult){
        result.success(true)
      }else{
        result.success(false)
      }
    }else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  //FUNCTION TO GENERATE KEY TO SIGN/VERIFY DATA
  private fun generateKey() {
    if(isDeviceSecure){
      //We create the start and expiry date for the key
      val startDate = GregorianCalendar()
      val endDate = GregorianCalendar()
      endDate.add(Calendar.YEAR, 1)

      //We are creating a RSA key pair and store it in the Android Keystore
      val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)

      //We are creating the key pair with sign and verify purposes
      val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(KEY_ALIAS,
        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY).run {
        setCertificateSerialNumber(BigInteger.valueOf(777))       //Serial number used for the self-signed certificate of the generated key pair, default is 1
        setCertificateSubject(X500Principal("CN=$KEY_ALIAS"))     //Subject used for the self-signed certificate of the generated key pair, default is CN=fake
        setDigests(KeyProperties.DIGEST_SHA256)                         //Set of digests algorithms with which the key can be used
        setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1) //Set of padding schemes with which the key can be used when signing/verifying
        setCertificateNotBefore(startDate.time)                         //Start of the validity period for the self-signed certificate of the generated, default Jan 1 1970
        setCertificateNotAfter(endDate.time)                            //End of the validity period for the self-signed certificate of the generated key, default Jan 1 2048
        setUserAuthenticationRequired(true)                             //Sets whether this key is authorized to be used only if the user has been authenticated, default false
        setUserAuthenticationValidityDurationSeconds(10)                //Duration(seconds) for which this key is authorized to be used after the user is successfully authenticated
        build()
      }

      //Initialization of key generator with the parameters we have specified above
      keyPairGenerator.initialize(parameterSpec)

      //Generates the key pair
      keyPair = keyPairGenerator.genKeyPair()
    }
  }

  //FUNCTION TO CHECK IF SIGN/VERIFY KEY EXISTS
  private fun checkKeyExists(): Boolean {
    if(isDeviceSecure){
      //We get the Keystore instance
      val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
      }

      //We get the private and public key from the keystore if they exists
      val privateKey: PrivateKey? = keyStore.getKey(KEY_ALIAS, null) as PrivateKey?
      val publicKey: PublicKey? = keyStore.getCertificate(KEY_ALIAS)?.publicKey

      return privateKey != null && publicKey != null
    }else{
      return false
    }
  }

  //FUNCTION TO VERIFY DATA READ FROM SHARED PREFERENCES
  private fun verifyData(dataToVerify: String?) : Boolean {
    //We get the Keystore instance
    if(isDeviceSecure){
      val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
      }
      val signatureFromUser = dataToVerify?.subSequence(0, dataToVerify.indexOf(":")).toString()
      val dataFromUser = dataToVerify?.subSequence(dataToVerify.indexOf(":")+1, dataToVerify.length).toString()

      //We get the certificate from the keystore
      val certificate: Certificate? = keyStore.getCertificate(KEY_ALIAS)

      if (certificate != null) {
        //We decode the signature value
        val signature: ByteArray = Base64.decode(signatureFromUser, Base64.DEFAULT)

        //We check if the signature is valid. We use RSA algorithm along SHA-256 digest algorithm
        val isValid: Boolean = Signature.getInstance("SHA256withRSA").run {
          initVerify(certificate)
          update(dataFromUser.toByteArray())
          verify(signature)
        }
        return isValid
      }else{
        return false
      }
    }else{
      return false
    }

  }

  //FUNCTION TO CATCH AUTHENTICATION RESULT
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (requestCode == REQUEST_CODE_FOR_CREDENTIALS) {
      if (resultCode == Activity.RESULT_OK) {
        //We get the Keystore instance
        val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
          load(null)
        }
        //Retrieves the private key from the keystore
        val privateKey: PrivateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
        //We sign the data with the private key. We use RSA algorithm along SHA-256 digest algorithm
        val signature: ByteArray? = Signature.getInstance("SHA256withRSA").run {
          initSign(privateKey)
          update(dataToSign.toByteArray())
          sign()
        }
        if (signature != null) {
          //We encode and store in a variable the value of the signature
          signatureResult = Base64.encodeToString(signature, Base64.DEFAULT)
          dataSignature = signatureResult
          val stringConcat = "$signatureResult:$dataToSign"
          pendingResult.success(stringConcat)
        }
        return true
      } else {
        Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
        pendingResult.success(false)
        activity.finish()
        return false
      }
    }
    else{
      return false
    }
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    TODO("Not yet implemented")
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    TODO("Not yet implemented")
  }

  override fun onDetachedFromActivity() {
    TODO("Not yet implemented")
  }


}
//KEYSTORE NAME
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
//SIGN/VERIFY KEY ALIAS
private const val KEY_ALIAS = "UserKey"
//REQUEST CODE FOR AUTHENTICATION SCREEN
const val REQUEST_CODE_FOR_CREDENTIALS = 1