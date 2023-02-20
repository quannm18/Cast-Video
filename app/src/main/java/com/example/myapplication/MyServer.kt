package com.example.myapplication

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

class MyServer(private val context: Context, private val port: Int ) : NanoHTTPD(port){
    override fun serve(session: IHTTPSession?): Response {
        return newFixedLengthResponse("Hello world")
    }

    override fun start() {
        super.start()
        val keyStoreFile = ""
        val password = "123456"

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
//        keyStore.load(context.assets.open(keyStoreFile), password.toCharArray())

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, password.toCharArray())

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, SecureRandom())
        makeSecure1(sslContext.socketFactory)
        Log.e("HttpsServer", "Server started at port $port}")
    }

    private fun makeSecure1(sslSocketFactory: SSLSocketFactory) {
        val sslParameters = SSLParameters()
        sslParameters.protocols = arrayOf("TLSv1.2")

        val httpsParams = HttpsParams()
        httpsParams.sslSocketFactory = sslSocketFactory
        httpsParams.needClientAuth = false
        httpsParams.clientAuth = ClientAuth.NONE
        httpsParams.sslParameters = sslParameters
        makeSecure2(httpsParams)
    }

    private fun makeSecure2(httpsParams: HttpsParams) {
        try {
            makeSecure3(httpsParams.sslSocketFactory)
        } catch (ioException: IOException) {
            throw IllegalStateException("Could not create secure server socket", ioException)
        }

    }


    private fun makeSecure3(sslSocketFactory: SSLSocketFactory): SSLServerSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {

            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
            }

            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {

            }
            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return sslContext.serverSocketFactory
    }



}

class HttpsParams {
    lateinit var sslSocketFactory: SSLSocketFactory
    var needClientAuth: Boolean = false
    var clientAuth: ClientAuth = ClientAuth.NONE
    lateinit var sslParameters: SSLParameters
}

enum class ClientAuth {
    NONE, WANT, NEED
}