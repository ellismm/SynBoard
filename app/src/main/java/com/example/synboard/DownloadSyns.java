package com.example.synboard;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import android.content.Context;

public class DownloadSyns {

    HashMap<String, ArrayList<String>> theSyns = new HashMap<>();
    HashMap<String, String> contractionWords = new HashMap<>();
    String line;

    public DownloadSyns(Context context) {
        int index = 0;

//        File file = new File("synonyms.txt");


//        try {
//            InputStream iS = context.getAssets().open("synonyms.txt");
//            BufferedReader buf = new BufferedReader(new InputStreamReader(iS));
////            System.out.println("I'm At least here6 \n\n\n\n");
//            String line;
//            while((line = buf.readLine()) != null) {
//                ArrayList<String> temp = new ArrayList<>();
//                String temp3 = line;
//                line = buf.readLine();
//                String[] temp2 = line.split(", ");
//                index = 0;
//                for(String s : temp2) {
//                    temp.add(s);
//                    if(++index == 31)
//                        break;
//                }
//                theSyns.put(temp3, temp);
//
////                for(String s : temp) {
////                    System.out.print(s + " ");
////                }
////                System.out.println(temp3);
////                break;
//            }
//
//        }
//        catch (IOException e) {
////            System.out.println("It didn't work");
//            e.printStackTrace();
//        }

        try {
            InputStream iS = context.getAssets().open("contractions.txt");
            BufferedReader buf = new BufferedReader(new InputStreamReader(iS));
            while((line = buf.readLine()) != null) {
                String temp = buf.readLine();
                contractionWords.put(line, temp);
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }


    }
}
