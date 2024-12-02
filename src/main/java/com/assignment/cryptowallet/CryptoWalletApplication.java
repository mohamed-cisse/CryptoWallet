package com.assignment.cryptowallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

@SpringBootApplication
public class CryptoWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoWalletApplication.class, args);
    }

}
