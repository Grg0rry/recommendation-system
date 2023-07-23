package solution;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper;

import org.apache.hadoop.mapreduce.lib.chain.ChainMapper;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.omg.CORBA.Context;

import solution.MoviesVector.MoviesVectorMapper;
import solution.MoviesVector.MoviesVectorReducer;
import solution.MoviesVector.MoviesVectorReducer.UserRating;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;


public class MoviesVector {

  /* Mapper */
  public static class MoviesVectorMapper extends Mapper<LongWritable, Text, Text, Text> {
    
    @Override
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
      
      context.write(new Text(""), value);
    }
  }

  /* Reducer */
  public static class MoviesVectorReducer extends Reducer<Text, Text, Text, Text> {

    public static final Map<String, Integer> UserRatingsByOrder = new HashMap<>();
    public static final List<String> UserList = new ArrayList<>();

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

      // List<String> UserList = new ArrayList<>();
      // Map<String, List<UserRating>> MovieRating = new HashMap<>();

      for (Text value : values) {
        String[] items = value.toString().split("\t", 2);

        if (items[0].startsWith("$User_List")){
          UserList.add(items[1]);
        } else {
          String[] user_rating = items[1].trim().split(":");
          String MovieTitle = items[0].toString();
          int UserID = Integer.parseInt(user_rating[0]);
          int Rating = Integer.parseInt(user_rating[1]);

          List<UserRating> userRatings = MovieRating.getOrDefault(MovieTitle, new ArrayList<>());
          // userRatings.add(new UserRating(UserID, Rating));
          userRatings.add(Rating);
          MovieRating.put(MovieTitle, userRatings);
        }
      }

      List<String> Vector = MovieRating.entrySet().stream()
      .filter(entry -> entry.getValue().size() >= 1000)
      .map(entry -> entry.getKey() + ":" + entry.getValue().stream().map(Object::toString).collect(Collectors.joining(",")))
      .collect(Collectors.toList());

      StringJoiner vectorBuilder = new StringJoiner(",");
      vectorBuilder.append(Vector.stream().map(Object::toString).collect(Collectors.joining(",")));
      
      context.write(key, new Text(vectorBuilder.toString()));
    }


      // for (Map.Entry<String, List<UserRating>> entry : MovieRating.entrySet()){
      //   String MovieTitle = entry.getKey();
      //   List<UserRating> UserRating = entry.getValue();

      //   if (UserRating.size() >= 1000) {
      //     Map<String, Integer> UserRatingsByOrder = new HashMap<>();
      //     for (String Order_UserID : UserList) {
      //       UserRatingsByOrder.put(Order_UserID, 0);
      //     }

      //     for (UserRating userRating : UserRating) {
      //       int UserID = userRating.getUserID();
      //       int Rating = userRating.getRating();
            
      //       if (UserRatingsByOrder.containsKey(String.valueOf(UserID))){
      //         UserRatingsByOrder.put(String.valueOf(UserID), Rating);
      //       }
      //     }

      //     List<Integer> Vector = new ArrayList<>(UserRatingsByOrder.values());
      //     StringBuilder vectorBuilder = new StringJoiner(",");
      //     for (Integer value : Vector) {
      //       vectorBuilder.add(value.toString());
      //     }     

      //     context.write(new Text(MovieTitle), new Text(vectorBuilder.toString()));
      //   } 
      // }
      
    class UserRating {
      private int UserID;
      private int Rating;

      public UserRating(int UserID, int Rating) {
        this.UserID = UserID;
        this.Rating = Rating;
      }

      public int getUserID(){
        return UserID;
      }

      public int getRating(){
        return Rating;
      }
    }
  }

  /* Driver */
  public static void main(String[] args) throws Exception {
      
    Job job = new Job();

    job.setJarByClass(MoviesVector.class);
    job.setJobName("MoviesVector");
    
    MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, MoviesVectorMapper.class);
    MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, MoviesVectorMapper.class);
    FileOutputFormat.setOutputPath(job, new Path(args[2]));

    // FileInputFormat.setInputPaths(job, new Path(args[0]));
    // FileOutputFormat.setOutputPath(job, new Path(args[1]));

    job.setMapperClass(MoviesVectorMapper.class);
    job.setReducerClass(MoviesVectorReducer.class);

    job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
          
    job.waitForCompletion(true);
  }
}