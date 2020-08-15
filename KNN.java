/*--------------------------------------------------------
1.Tsai-Ting,Yeh 2020/06/05

2.java build 1.8.0_191 

3.(before you start it need to change the path the data folder and put the file in the same folder)
command line compilation examples:

>javac KNN.java

4.examples to run this program:

In shell window:

> java KNN


6. The file submit and you need for the program.

 a. KNN.java
 b. knn-csc480-a4.csv

 
7. Note: Need to change the file format "knn-csc480-a4.xls" TO "knn-csc480-a4.csv"


----------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class KNN {
	public static void main(String[] args) {
		
		//initial ArrayList userRatings and newUserRatings for later use
		ArrayList<Integer[]> userRatings = new ArrayList<>();
    	ArrayList<Integer[]> newUserRatings = new ArrayList<>();	    		    	
    	
    	File directory = new File("");
		
    	//make the filePath from current directory
    	String filePath=directory.getAbsolutePath()+"/knn-csc480-a4.csv";
		//display the read file path
		System.out.println("Read from: "+filePath);
		String line = "";
    	//since read cvs it will separate data by , 
		//we need to remove that and get the data
        String cvsSplitBy = ",";
        int count=0;
        Integer[] ratings;
        String[] read;
        boolean users = true;
        
        //open the file and fill data to the array list
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            while ((line = br.readLine()) != null) {
            	// ensure last comma is split
            	// original it will ignore the last blank
            	line=line+" ";
                // use comma as separator
                read = line.split(cvsSplitBy);
                //for later fill rating
                ratings=new Integer[read.length-1];
                
                //skip the first row
                if(count!=0) {
                	for(int i=0;i<read.length;i++) {
                		//iteration the read in data if it is blank, so we fill it to the number 0
                		String s=read[i];
                		if(s.equals("")||s.equals(" ")) {
                			read[i]="0";              			
                		}
                	}
                	
	                //Make the read in data numerical
					for (int i = 1; i < read.length; i++) {
						ratings[i - 1] = Integer.parseInt(read[i].trim());
					}
					//in the original file, it has the empty line
					//to separate the original user and the new user
					if (line.matches("[^0-9]*")) {
						//change it to false, so we can know it will continue to read the new user
						users = false;
						//skip the empty line
						continue; 
					}	                
	            	//System.out.println(Arrays.toString(ratings));
					//if data read in is original given user, so add it to the userRating arraylist
					if (users) {
						userRatings.add(ratings);
					} else {
					//or it will be the new user, so we add it to the newUserRating arraylist
						newUserRatings.add(ratings);
					}						
                }	    
               count++;
            }	            
            System.out.println("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        
		//Make a format table of mean absolute error for various values of k
		System.out.printf("%5s  %10s%n", "k", "MAE");
		int bestK=0;
		Double min=Double.MAX_VALUE;
		for (int k = 1; k <= 20; k++) {
			//put different number of k into meanAbsoluteError
			double mae = meanAbsoluteError(userRatings, newUserRatings, k);
			//use this compare to find the best k
			if(mae<min) {
				min=mae;
				bestK=k-1;
			}
			System.out.printf("%5d  %10.2f%n", k, mae);
		}
		System.out.println("Best K is : "+bestK);
		System.out.println("");
		
		//Compute predictions for unrated items
		for (int i = 0; i <= 1; i++) {
			Integer[] newUserRating = newUserRatings.get(i);
			for (int j = 0; j < newUserRating.length; j++) {
				//iteration the newUserRatings list and if it equal to 0, it is means unrated
				if (newUserRating[j] == 0) {
					int[] temp=Arrays.stream(newUserRating).mapToInt(Integer::intValue).toArray();
					double rate = knn(bestK,temp, userRatings, j);
					System.out.println("NU"+(i+1)+" unrated items :"+ j+ "'s rate is "+rate);
				}
			}
		}
		System.out.println("");
		//Find best predictions for the specified users
		//U2, U5, U13, U20
		int[] indices = { 1, 4, 12, 19 };
		
		for (int u : indices) {
			ArrayList<Double> predictions = new ArrayList<>();
			ArrayList<Integer> bestIndices = new ArrayList<>();
			//Suggestions for user u are based on similarity of other users and their ratings,
			//not user u itself

			ArrayList<Integer[]> temp = new ArrayList<>(userRatings);
			//so we remove the specified users from the temp Array list
			temp.remove(u);
			//get the rating from specified users
			Integer[] user = userRatings.get(u);
			
			outer: for (int i = 0; i < user.length; i++) {
				if (user[i] == 0) {
					int[] temp2=Arrays.stream(user).mapToInt(Integer::intValue).toArray();
					//get the prediction from KNN
					double prediction = knn(4, temp2, temp, i);
					for (int j = 0; j < predictions.size(); j++) {
						if (prediction > predictions.get(j)) {
							predictions.add(j, prediction);
							bestIndices.add(j, i);
							//if prediction is greater than 3
							if (predictions.size() > 3) {
								predictions.remove(predictions.size() - 1);
								bestIndices.remove(bestIndices.size() - 1);
							}
							continue outer;
						}
					}					
					predictions.add(prediction);
					bestIndices.add(i);

				}
				
			}	
			System.out.println("Recommendations for user "+u+" : "+bestIndices.toString());
		}

	}
	
		//Target user is the array of ratings by that user across items
		//Target index is the index of the item for which a prediction is required
		//returns predicted rating of targetUser for item at targetIndex
		public static double knn(int k, int[] targetUser, ArrayList<Integer[]> users, int targetIndex) {
			//initial the arry list nearestIndices and coefficients for later use
			ArrayList<Integer> nearestIndices = new ArrayList<>();
			ArrayList<Float> coefficients = new ArrayList<>();

			//Determine the k nearest neighbor users identified by their indices
			for (int i = 0; i < users.size(); i++) {
				//get the int list from Integer Array List
				int[] temp=Arrays.stream(users.get(i)).mapToInt(Integer::intValue).toArray();
				//use the functiom correlationCoefficient to calculate correlation coefficient
				Float coeff = correlationCoefficient(temp,targetUser, targetUser.length, targetIndex);	
				int j;
				for (j = 0; j < nearestIndices.size(); j++) {
					if (coeff < coefficients.get(j)) {
						break;
					}
				}
				//add Integer i to the nearestIndices at index j 
				nearestIndices.add(j, i);
				//record the coefficient at index j
				coefficients.add(j, coeff);
				
				//the nearestIndices and coefficients size is depend on k
				if (nearestIndices.size() > k) {
					//if greater than k, so remove it
					nearestIndices.remove(nearestIndices.size() - 1);
					coefficients.remove(coefficients.size() - 1);
				}
			}

			//Calculate the predicted rating numerator and denominator
			double denominator = 0;
			double numerator = 0;
			
			for (int j = 0; j < nearestIndices.size(); j++) {
				//fill the list of neighbor use the nearestIndices list and get from users list
				Integer[] neighbor = users.get(nearestIndices.get(j));
				if (neighbor[targetIndex] > 0) {
					//use the formula to count
					double sim = coefficients.get(j);
					denominator += sim;
					numerator += sim * neighbor[targetIndex];
				}
			}

			// It may be that the similar users do not have a rating to predict the targetIndex with
			// In such a case, simply predict a rating of 0
			if (denominator == 0) {
				return 0;
			}
			double result=numerator / denominator;
			return result;
		}

		// Determine the mean absolute error
		public static double meanAbsoluteError(ArrayList<Integer[]> users, ArrayList<Integer[]> newUsers, int k) {
			//initial the totalAbsoluteError 
			double totalAbsoluteError = 0;
			int count = 0;
			for (Integer[] newUser : newUsers) {
				for (int i = 0; i < newUser.length; i++) {
					if (newUser[i] > 0) {
						int[] temp=Arrays.stream(newUser).mapToInt(Integer::intValue).toArray();
						//get the rating from knn function and compare it with actual rating
						double rating = knn(k, temp, users, i);
						totalAbsoluteError += Math.abs(rating - newUser[i]);
						//increase the count for later use
						count++;
					}
				}
			}
			//average totalAbsoluteError to get the MAE.
			double result=totalAbsoluteError / count;
			return result;
		}

		// use itemIndex to calculate similarity without considering the users' ratings
		// for that particular item, function that returns correlation coefficient.
		public static float correlationCoefficient(int X[], int Y[], int n, int itemIndex) {
			//initial the variable
			int sumX = 0;
			int sumY = 0;
			int sumXY = 0;
			int squareSumX = 0;
			int squaresumY = 0;

			for (int i = 0; i < n; i++) {
				// Only consider overlapping ratings
				if (X[i] != 0 && Y[i] != 0 && i != itemIndex) {
					// this is sum of elements of array X.
					sumX = sumX + X[i];

					// this is sum of elements of array Y.
					sumY = sumY + Y[i];

					//This is sum of X[i] * Y[i].
					sumXY = sumXY + X[i] * Y[i];

					//This is sum of square of array elements.
					squareSumX = squareSumX + X[i] * X[i];
					squaresumY = squaresumY + Y[i] * Y[i];
				}
			}

			//we use formula for calculating correlation
			//coefficient.
			float corr = (float) (n * sumXY - sumX * sumY) / (float) (Math.sqrt((n * squareSumX - sumX * sumX) * (n * squaresumY - sumY * sumY)));
			return corr;
		}
}
