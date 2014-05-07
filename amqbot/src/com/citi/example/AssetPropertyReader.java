package com.citi.example;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.citi.example.bot.Main;

public class AssetPropertyReader {
       private Properties properties = new Properties();

       public AssetPropertyReader(String fileName) {
    	   try {
			FileInputStream f = new FileInputStream(fileName);
			properties.load(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
       }

       public Properties getProperties() {
              return properties;
       }
}
