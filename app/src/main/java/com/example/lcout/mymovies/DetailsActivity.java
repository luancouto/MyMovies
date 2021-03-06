package com.example.lcout.mymovies;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.lcout.mymovies.data.FavouriteContract;
import com.example.lcout.mymovies.model.Movie;
import com.example.lcout.mymovies.model.Review;
import com.example.lcout.mymovies.model.Video;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DetailsActivity extends AppCompatActivity implements VideoAdapter.ListItemClickListener {

    private final String TAG = DetailsActivity.class.getSimpleName();
    private Movie mMovie;
    private ArrayList<Video> mVideos;
    private ArrayList<Review> mReviews;

    private final String baseURL = "http://api.themoviedb.org/3";
    private final String trailers = "/movie/%s/videos";
    private final String reviews = "/movie/%s/reviews";
    private final String parameterKey = "api_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        getMovieFromExtra();
        pupulateScreenInfo();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addFavouriteMovie();
            }
        });

        if (!isOnline()) {
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
            return;
        }
        getTrailers();
        getReviews();
    }

    private void getReviews() {
        final String movieApiKey = getResources().getString(R.string.movie_key);
        Uri builtUri =
                Uri.parse(baseURL + String.format(reviews, mMovie.id.toString()))
                        .buildUpon()
                        .appendQueryParameter(parameterKey, movieApiKey)
                        .build();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, builtUri.toString(), null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG + " Volley reviews", new Gson().toJson(response));
                        mReviews = getReviewsFromJson(response);
                        prepareReviewsRecyclerView();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG + " Error Volley reviews", error.toString());
                    }
                });
        Volley.newRequestQueue(this).add(jsonObjectRequest);

    }

    private void getTrailers() {
        final String movieApiKey = getResources().getString(R.string.movie_key);
        Uri builtUri =
                Uri.parse(baseURL + String.format(trailers, mMovie.id.toString()))
                        .buildUpon()
                        .appendQueryParameter(parameterKey, movieApiKey)
                        .build();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, builtUri.toString(), null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG + " Volley videos", new Gson().toJson(response));
                        mVideos = getVideosFromJson(response);
                        prepareVideosRecyclerView();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG + " Error Volley videos", error.toString());
                    }
                });
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void prepareVideosRecyclerView() {
        RecyclerView rvVideos = findViewById(R.id.rv_videos);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvVideos.setLayoutManager(layoutManager);
        rvVideos.setHasFixedSize(true);
        VideoAdapter videoAdapter = new VideoAdapter(mVideos, this, this);
        rvVideos.setAdapter(videoAdapter);
    }

    private void prepareReviewsRecyclerView() {
        RecyclerView rvReviews = findViewById(R.id.rv_reviews);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvReviews.setLayoutManager(layoutManager);
        rvReviews.setHasFixedSize(true);
        ReviewAdapter reviewAdapter = new ReviewAdapter(mReviews, this);
        rvReviews.setAdapter(reviewAdapter);
    }

    private ArrayList<Video> getVideosFromJson(JSONObject response) {
        if (response == null || response.equals(""))
            return null;

        ArrayList<Video> tempVideos = new ArrayList<>();

        try {
            JSONArray videosJSON = response.getJSONArray("results");

            for (int i = 0; i < videosJSON.length(); i++) {
                JSONObject video = videosJSON.getJSONObject(i);

                String name = video.getString("name");
                String key = video.getString("key");
                Video temp = new Video();
                temp.name = name;
                temp.key = key;

                tempVideos.add(temp);

                Log.d(TAG, "name: " + name);
                Log.d(TAG, "key: " + key);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return tempVideos;
    }

    private ArrayList<Review> getReviewsFromJson(JSONObject response) {
        if (response == null || response.equals(""))
            return null;

        ArrayList<Review> tempReviews = new ArrayList<>();

        try {
            JSONArray reviewsJSON = response.getJSONArray("results");

            for (int i = 0; i < reviewsJSON.length(); i++) {
                JSONObject review = reviewsJSON.getJSONObject(i);

                String author = review.getString("author");
                String content = review.getString("content");
                Review temp = new Review();
                temp.author = author;
                temp.review = content;

                tempReviews.add(temp);

                Log.d(TAG, "author: " + author);
                Log.d(TAG, "content: " + content);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return tempReviews;
    }

    private void addFavouriteMovie() {
        if (mMovie == null || mMovie.id < 1)
            return;

        ContentValues contentValues = getValuesFromMovie();

        try { //Try to INSERT
            Uri uri = getContentResolver().insert(FavouriteContract.FavouriteEntry.CONTENT_URI, contentValues);
            if (uri != null) {
                Log.d(TAG, contentValues.toString());
                Toast.makeText(getBaseContext(), "Added to Favorite! :)", Toast.LENGTH_SHORT).show();
            }
        } catch (android.database.SQLException ex) {

            //Already in the DB, so try to DELETE
            String movieID = mMovie.id.toString();
            Uri uri = FavouriteContract.FavouriteEntry.CONTENT_URI;
            uri = uri.buildUpon().appendPath(movieID).build();

            try {
                int moviesDeleted = getContentResolver().delete(uri, null, null);
                if (moviesDeleted > 0)
                    Toast.makeText(getBaseContext(), "Removed from Favorite! :(", Toast.LENGTH_SHORT).show();

            } catch (Exception exDel) {
                    Toast.makeText(getBaseContext(), "ALERT: ERROR", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, exDel.getMessage());
            }
        }
    }

    private ContentValues getValuesFromMovie() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FavouriteContract.FavouriteEntry.COLUMN_MOVIE_ID, mMovie.id);
        contentValues.put(FavouriteContract.FavouriteEntry.COLUMN_TITLE, mMovie.title);
        contentValues.put(FavouriteContract.FavouriteEntry.COLUMN_OVERVIEW, mMovie.overview);
        contentValues.put(FavouriteContract.FavouriteEntry.COLUMN_POSTER_PATH, mMovie.poster_path);
        contentValues.put(FavouriteContract.FavouriteEntry.COLUMN_RELEASE_DATE, mMovie.release_date);
        contentValues.put(FavouriteContract.FavouriteEntry.COLUMN_VOTE_AVERAGE, mMovie.vote_average);
        return contentValues;
    }

    private void pupulateScreenInfo() {
        TextView title = findViewById(R.id.tv_title);
        ImageView thumbnail = findViewById(R.id.iv_thumbnail);
        TextView overview = findViewById(R.id.tv_overview);
        TextView rating = findViewById(R.id.tv_rating);
        TextView release_date = findViewById(R.id.tv_release);

        title.setText(mMovie.title);
        String imgBaseURL = "http://image.tmdb.org/t/p/";
        String imgSize = "w185/";
        String fullImageURL = imgBaseURL + imgSize + mMovie.poster_path;
        Picasso.with(this).load(fullImageURL).into(thumbnail);
        overview.setText(mMovie.overview);
        overview.setMovementMethod(new ScrollingMovementMethod());
        rating.setText(mMovie.vote_average.toString());
        release_date.setText(mMovie.release_date);
    }

    private void getMovieFromExtra() {
        Intent intent = getIntent();
        if (!intent.hasExtra(Intent.EXTRA_TEXT))
            finish();

        String extra = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (TextUtils.isEmpty(extra))
            finish();

        Log.d(TAG + " LUAN", extra);
        Gson gson = new Gson();
        mMovie = gson.fromJson(extra, Movie.class);
    }

    @Override
    public void onListItemClick(int clickedIndex) {
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + mVideos.get(clickedIndex).key));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + mVideos.get(clickedIndex).key));
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            startActivity(webIntent);
        }
    }

    //TODO: best to move it to an util class
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
