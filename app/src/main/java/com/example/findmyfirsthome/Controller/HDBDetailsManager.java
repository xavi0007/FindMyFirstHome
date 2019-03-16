package com.example.findmyfirsthome.Controller;
import com.example.findmyfirsthome.Entity.HDBDevelopment;
import com.example.findmyfirsthome.Entity.HDBFlatType;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


//Startup Controller just call write data.

public class HDBDetailsManager extends AsyncTask<String, Void, Void>  {

    //variables init;
    //url to be given by startUp controller
    private String urlDetails = "http://esales.hdb.gov.sg/bp25/launch/19feb/bto/19FEBBTO_page_6280/$file/about0.html"; //need to get more of this, each development have different details url
    private String urlMain = "http://esales.hdb.gov.sg/bp25/launch/19feb/bto/19FEBBTO_page_6280/$file/about0.html";
    private ProgressDialog mProgressDialog;
    private ArrayList<String> HDBDevelopmentNames = new ArrayList<String>();
    private ArrayList<String> temp;
    private ArrayList<HashMap<String, Object>>ListFlatTypePrice = new ArrayList<HashMap<String, Object>>();
    private String descriptionText;


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog.setTitle("Scraping from HDB now");
        mProgressDialog.setMessage("Loading...");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.show();
    }

    //scrap data happens here
    @Override
    protected Void doInBackground(String... url) {

        //BoonLay & Jurong west
        temp = (scrapDevelopmentName(urlMain, 0, 3, 1)); //scrap from table 0, 4th row 2nd data;
        addToList(temp,HDBDevelopmentNames);
        ListFlatTypePrice.add((scrapFlatType(urlMain,0 ,3,4, 8 )));
        //SK
        temp = (scrapDevelopmentName(urlMain, 0, 8, 1));
        addToList(temp,HDBDevelopmentNames);
        ListFlatTypePrice.add((scrapFlatType(urlMain,0,8,9, 13)));
        //Kallang
        temp = (scrapDevelopmentName(urlMain, 0, 4, 1));
        addToList(temp,HDBDevelopmentNames);
        ListFlatTypePrice.add((scrapFlatType(urlMain,0,8,9, 13)));
        print(HDBDevelopmentNames);

        try {
            descriptionText = description(urlDetails, "Jurong West Jewel", "Boon Lay Glade");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        mProgressDialog.dismiss();
    }


    public ArrayList<HDBDevelopment> getHDBData(){
        return createHDBDevelopment();
    }


    protected ArrayList<HDBDevelopment> createHDBDevelopment(){
        int index = 0;
        HDBDevelopment HDBD = null;
        ArrayList<HDBDevelopment> HDBDList = new ArrayList<HDBDevelopment>();
        if(HDBDevelopmentNames.isEmpty()){
            return null;
        }else{
            while(HDBDevelopmentNames.get(index) != null){
                HDBD = new HDBDevelopment(ListFlatTypePrice, HDBDevelopmentNames.get(index), descriptionText,
                        false, null, null);
                HDBDList.add(HDBD);
                index++;
            }
        }
        return HDBDList;
    }

    //TODO: A while loop to write data as the data is called asynchronously.

    public void writeData(){
        dbWritesData(createHDBDevelopment());
    }
    //hold this first
    protected boolean dbWritesData(ArrayList<HDBDevelopment> developments){
        MapsController mc = new MapsController();
        Context context = mc.getContext();
        DatabaseController db = new DatabaseController(context);
        //get flattype
        //create faltType in the class
        //use writeData to enter flat type

        for(HDBDevelopment hdb : developments){
            db.writeHDBata(hdb);
        }
        return true;
    }


    protected String description(String url, String developmentName1, String developmentName2) throws IOException {
        Document document = Jsoup.connect(url).get();
        for(int i = 0; i <  document.select("p").size(); i ++) {
            Element paragraphs = document.select("p").get(i);
            if(paragraphs.text().length() > 150) {
                String description = paragraphs.text();
                if(description.contains(developmentName1)) {
                    return(developmentName1 + ": " + description);
                }else if(description.contains(developmentName2)) {
                    System.out.println('\n');
                    return('\n' + developmentName2 + ": " + description);
                }

            }
        }
        return "";
    }

    //overloading cuz HDB a bitch
    protected String description(String url, String developmentName1) throws IOException {
        Document document = Jsoup.connect(url).get();
        for (int i = 0; i < document.select("p").size(); i++) {
            Element paragraphs = document.select("p").get(i);
            if (paragraphs.text().length() > 150) {
                String description = paragraphs.text();
                if (description.contains(developmentName1)) {
                    return(developmentName1 + ": " + description);
                }
            }
        }

        return "";
    }

    protected HashMap<String, Object> scrapFlatType(String url, int tableNumber, int firstRowNumber, int rowStart, int rowEnd){


        HashMap<String, Object> flatType = new HashMap<String, Object>();
        try {
            //Connect to the page
            Document document = Jsoup.connect(url).get();
            Element table = document.select("table").get(tableNumber); //select the first table.

            Elements rows = table.select("tr");
            //For 2-room because HDB wannabe a special snowflake
            //get(3) for Boonlay
            //get(8) for SK
            //get(14) for  kallang
            Element row = rows.get(firstRowNumber);
            Elements cols = row.select("td");

            String rooms = cols.get(2).text();
            String price = cols.get(3).text();
            flatType.put(rooms, price);
            //get(3) for Boonlay => 4 ~ 8
            //get(8) for SK => 9~13
            //get(14) for Kallang => 15~16
            for(int i = rowStart; i < rowEnd; i++) {
                row = rows.get(i);

                cols = row.select("td");

                rooms = cols.get(0).text();
                price = cols.get(1).text();
                flatType.put(rooms,price);

            }

        }catch (IOException e) {
            e.printStackTrace();
        }


        return flatType;
    }

    protected ArrayList<String> scrapDevelopmentName(String url, int tableNumber, int rowNumber, int colNumber) {
        try {
            ArrayList<String> textList = new ArrayList<String>();
            Document document = Jsoup.connect(url).get();

            Element table = document.select("table").get(tableNumber); //select the first table.

            Elements rows = table.select("tr");
            Element row = rows.get(rowNumber);

            Elements cols = row.select("td");

            String text = cols.get(colNumber).html();

            if(text.contains("<br>")) {
                String[] textArray= text.split("<br>");
                for(int i =0 ; i < textArray.length ; i++) {
                    textList.add(textArray[i]);
                }
            }
            if (text.contains("<sup>")) {
                text = cols.get(colNumber).text();
                textList.add(text);
            }
            return textList;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected void addToList(ArrayList<String> temp, ArrayList<String> target) {
        for(int i = 0; i < temp.size() ; i++ ) {
            target.add(temp.get(i));
        }
    }
//    ----------------------------DEBUG PRINT METHODS----------------------------
    public void print(ArrayList<String> stringList) {
        System.out.println("-----------------Inside the arrayList-----------------");
        for (int i = 0; i < stringList.size(); i++) {
            System.out.println("Index: " + i + " :" + stringList.get(i));
        }
    }

    public void print(HashMap<String, String> hashList) {
        System.out.println("-----------------Inside the hashmap-----------------");
        for (String key: hashList.keySet()){
            System.out.println(key);
            System.out.println(hashList.get(key));
        }
    }
    public void print(String[] string, int i) {
        System.out.println("index: " + i + string[i]);
    }

}


//TODO:
///Need to scrap this and put in HashMap<String(for area i.e Ang Mo Kio), HashMap<String (For flat type), Double(Price)>>;
//https://www.hdb.gov.sg/cs/infoweb/residential/renting-a-flat/renting-from-the-open-market/rental-statistics
//This is to find the Annual value, rental of latest quarter * 12;

//TODO:
//For both 1st timer: https://www.hdb.gov.sg/cs/infoweb/residential/buying-a-flat/new/first-timer-applicants
//For 1 1st 1 2nd : https://www.hdb.gov.sg/cs/infoweb/residential/buying-a-flat/new/first-timer-and-second-timer-couple-applicants

