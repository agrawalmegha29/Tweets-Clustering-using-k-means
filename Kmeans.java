/**
*
* @author  Megha Agrawal

*/
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Megha
 */
public class Kmeans {

    static List<TweetData> tweetlist;
    static List<TweetData> lastcentroidlist;
    static List<clusterTweets> clusterlist;
    static int kData = 25;

    public static void readJSONData(String path) throws IOException {
        FileInputStream fin = new FileInputStream(path);
        BufferedReader inputBRJSON = new BufferedReader(new InputStreamReader(fin));
        String line;
        ArrayList<String> s = new ArrayList();
        while ((line = inputBRJSON.readLine()) != null) {
            s.add(line);
        }
        for (String item : s) {
            TweetData t = new TweetData();
            String[] l = item.split("\"text\":");
            String[] m = item.split("\"id\":");
            t.text = l[1].substring(1, l[1].indexOf(','));
            t.id = Long.parseLong(m[1].substring(1, m[1].indexOf(','));
            tweetlist.add(t);
        }
       
    }

    public static void getInitialSeedData(String path) throws IOException {
        FileInputStream fin = new FileInputStream(path);
        BufferedReader inputBRSeed = new BufferedReader(new InputStreamReader(fin));
        String line;
        ArrayList<String> s = new ArrayList();
        while ((line = inputBRSeed.readLine()) != null) {
            s.add(line.replace(",", ""));
        }
        clusterlist = new ArrayList<>();
        for (int i = 0; i < kData; i++) {
            for (int j = 0; j < tweetlist.size(); j++) {
                if (Long.parseLong(s.get(i)) == tweetlist.get(j).id) {
                    clusterTweets cL = new clusterTweets(i);
                    cL.centroidTweets = tweetlist.get(j);
                    clusterlist.add(cL);

                }
            }
        }

    }

    public static double calculateJaccardDistance(String centroidTweets, String tweet) {
        List<String> a = Arrays.asList(centroidTweets.toLowerCase().split(" "));
        List<String> b = Arrays.asList(tweet.toLowerCase().split(" "));

        Set<String> union = new HashSet<String>(a);
        union.addAll(b);

        Set<String> intersection = new HashSet<String>(a);
        intersection.retainAll(b);

        return (double) (1 - (intersection.size() / (double) union.size()));

    }

    public static void clusterTweets() {

        flushClusters();
        ArrayList<Double[]> distancelist = new ArrayList();

        for (int i = 0; i < kData; i++) {

            Double[] distance = new Double[tweetlist.size()];

            for (int j = 0; j < tweetlist.size(); j++) {

                distance[j] = calculateJaccardDistance(clusterlist.get(i).centroidTweets.text, tweetlist.get(j).text);
            }

            distancelist.add(distance);
        }

        for (int i = 0; i < tweetlist.size(); i++) {
            Double min = Double.MAX_VALUE;
            int index = 0;
            for (int k = 0; k < kData; k++) {
                if (distancelist.get(k)[i] < min) {
                    min = distancelist.get(k)[i];
                    index = k;
                }

            }
            clusterlist.get(index).tweetList.add(tweetlist.get(i));

        }
    }

    public static void updateCentroids() {

        for (int i = 0; i < kData; i++) {
            int index = 0;
            double min = Double.MAX_VALUE;
            for (int j = 0; j < clusterlist.get(i).tweetList.size(); j++) {

                double distance = 0;

                for (int k = 0; k < clusterlist.get(i).tweetList.size(); k++) {
                    distance += calculateJaccardDistance(clusterlist.get(i).tweetList.get(j).getText(), clusterlist.get(i).tweetList.get(k).getText());
                }

                if (distance < min) {
                    min = distance;
                    index = j;
                }
            }
            clusterlist.get(i).centroidTweets = clusterlist.get(i).tweetList.get(index);

        }

    }

    public static void flushClusters() {
        lastcentroidlist = new ArrayList<>();
        for (int i = 0; i < clusterlist.size(); i++) {
            clusterlist.get(i).tweetList.clear();
            lastcentroidlist.add(clusterlist.get(i).centroidTweets);//preserve older centroids
        }
    }

    public static void printJaccardCluster(String outputfilepath, double sseValidation) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(outputfilepath, true))) {
            out.println("ClusterNo"  + "\t"+"TweetId");
            for (int i = 0; i < clusterlist.size(); i++) {

                out.print(i + 1 + "\t\t");
                String s = "";
                for (int j = 0; j < clusterlist.get(i).tweetList.size(); j++) {
                    s += clusterlist.get(i).tweetList.get(j).id.toString() + ",";
                }
                out.println(s);

            }
            out.println("The Sqaured Error is: " + sseValidation);
        }

    }

    public static double findSSEValidation() {
        double sseValidation = 0;
        for (clusterTweets cL : clusterlist) {
            double distance = 0;
            double tempDist = 0;
            for (int k = 0; k < cL.tweetList.size(); k++) {
                tempDist = calculateJaccardDistance(cL.centroidTweets.text, cL.tweetList.get(k).text);
                distance += (tempDist * tempDist);
            }
            sseValidation += distance;
        }
        return sseValidation;
    }
	
	public static void main(String[] args) throws IOException {
        // TODO code application logic here
        tweetlist = new ArrayList<TweetData>();
        kData = Integer.parseInt(args[0]);
        String initialSeedData = args[1];
        String inputJSONData = args[2];
        String outputFileData = args[3];
        readJSONData(inputJSONData);
        getInitialSeedData(initialSeedData);
        for (int j = 0; j < 25; j++) {
            clusterTweets();
            updateCentroids();
            List<TweetData> currentCentroidList = new ArrayList<>();
            double jaccardChange = 0;

            for (int i = 0; i < clusterlist.size(); i++) {

                currentCentroidList.add(clusterlist.get(i).centroidTweets);//new centroids
                jaccardChange += calculateJaccardDistance(lastcentroidlist.get(i).text, currentCentroidList.get(i).text);
            }

            if (jaccardChange == 0) {
                break;
            }
        }
        double sseValidation = findSSEValidation();
        printJaccardCluster(outputFileData, sseValidation);
        System.out.println("Squared Error is: " + sseValidation);
    }
}
