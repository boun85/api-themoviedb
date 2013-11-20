/*
 *      Copyright (c) 2004-2013 Stuart Boston
 *
 *      This file is part of TheMovieDB API.
 *
 *      TheMovieDB API is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      TheMovieDB API is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with TheMovieDB API.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.omertron.themoviedbapi.methods;

import com.omertron.themoviedbapi.MovieDbException;
import static com.omertron.themoviedbapi.methods.ApiUrl.PARAM_ID;
import static com.omertron.themoviedbapi.methods.ApiUrl.PARAM_SESSION;
import com.omertron.themoviedbapi.model.ListItemStatus;
import com.omertron.themoviedbapi.model.StatusCode;
import com.omertron.themoviedbapi.model.StatusCodeList;
import com.omertron.themoviedbapi.model.movie.MovieDbList;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.http.CommonHttpClient;

/**
 * Class to hold the Lists methods
 *
 * @author stuart.boston
 */
public class TmdbLists extends AbstractMethod {

    private static final Logger LOG = LoggerFactory.getLogger(TmdbLists.class);
    // API URL Parameters
    private static final String BASE_LIST = "list/";

    /**
     * Constructor
     *
     * @param apiKey
     * @param httpClient
     */
    public TmdbLists(String apiKey, CommonHttpClient httpClient) {
        super(apiKey, httpClient);
    }

    /**
     * Get a list by its ID
     *
     * @param listId
     * @return The list and its items
     * @throws MovieDbException
     */
    public MovieDbList getList(String listId) throws MovieDbException {
        ApiUrl apiUrl = new ApiUrl(apiKey, BASE_LIST);
        apiUrl.addArgument(PARAM_ID, listId);

        URL url = apiUrl.buildUrl();
        String webpage = requestWebPage(url);

        try {
            return MAPPER.readValue(webpage, MovieDbList.class);
        } catch (IOException ex) {
            LOG.warn("Failed to get list: {}", ex.getMessage());
            throw new MovieDbException(MovieDbException.MovieDbExceptionType.MAPPING_FAILED, webpage, ex);
        }
    }

    /**
     * Check to see if a movie ID is already added to a list.
     *
     * @param listId
     * @param movieId
     * @return true if the movie is on the list
     * @throws MovieDbException
     */
    public boolean isMovieOnList(String listId, Integer movieId) throws MovieDbException {
        ApiUrl apiUrl = new ApiUrl(apiKey, BASE_LIST, listId + "/item_status");
        apiUrl.addArgument("movie_id", movieId);

        URL url = apiUrl.buildUrl();
        String webpage = requestWebPage(url);

        try {
            return MAPPER.readValue(webpage, ListItemStatus.class).isItemPresent();
        } catch (IOException ex) {
            LOG.warn("Failed to process movie list: {}", ex.getMessage());
            throw new MovieDbException(MovieDbException.MovieDbExceptionType.MAPPING_FAILED, webpage, ex);
        }
    }

    /**
     * This method lets users create a new list. A valid session id is required.
     *
     * @param sessionId
     * @param name
     * @param description
     * @return The list id
     * @throws MovieDbException
     */
    public StatusCodeList createList(String sessionId, String name, String description) throws MovieDbException {
        ApiUrl apiUrl = new ApiUrl(apiKey, BASE_LIST.replace("/", ""));
        apiUrl.addArgument(PARAM_SESSION, sessionId);

        HashMap<String, String> body = new HashMap<String, String>();
        body.put("name", StringUtils.trimToEmpty(name));
        body.put("description", StringUtils.trimToEmpty(description));

        String jsonBody = convertToJson(body);

        URL url = apiUrl.buildUrl();
//        String webpage = requestWebPage(url, jsonBody);
        String webpage = postWebPage(url, jsonBody);

        try {
            return MAPPER.readValue(webpage, StatusCodeList.class);
        } catch (IOException ex) {
            LOG.warn("Failed to create list: {}", ex.getMessage());
            throw new MovieDbException(MovieDbException.MovieDbExceptionType.MAPPING_FAILED, webpage, ex);
        }
    }

    /**
     * This method lets users add new movies to a list that they created. A valid session id is required.
     *
     * @param sessionId
     * @param listId
     * @param movieId
     * @return true if the movie is on the list
     * @throws MovieDbException
     */
    public StatusCode addMovieToList(String sessionId, String listId, Integer movieId) throws MovieDbException {
        return modifyMovieList(sessionId, listId, movieId, "/add_item");
    }

    /**
     * This method lets users remove movies from a list that they created. A valid session id is required.
     *
     * @param sessionId
     * @param listId
     * @param movieId
     * @return true if the movie is on the list
     * @throws MovieDbException
     */
    public StatusCode removeMovieFromList(String sessionId, String listId, Integer movieId) throws MovieDbException {
        return modifyMovieList(sessionId, listId, movieId, "/remove_item");
    }

    private StatusCode modifyMovieList(String sessionId, String listId, Integer movieId, String operation) throws MovieDbException {
        ApiUrl apiUrl = new ApiUrl(apiKey, BASE_LIST, listId + operation);

        apiUrl.addArgument(PARAM_SESSION, sessionId);

        String jsonBody = convertToJson(Collections.singletonMap("media_id", movieId + ""));

        URL url = apiUrl.buildUrl();
        String webpage = requestWebPage(url, jsonBody);

        try {
            return MAPPER.readValue(webpage, StatusCode.class);
        } catch (IOException ex) {
            LOG.warn("Failed to modify movie list: {}", ex.getMessage());
            throw new MovieDbException(MovieDbException.MovieDbExceptionType.MAPPING_FAILED, webpage, ex);
        }
    }

    /**
     * This method lets users delete a list that they created. A valid session id is required.
     *
     * @param sessionId
     * @param listId
     * @return
     * @throws MovieDbException
     */
    public StatusCode deleteMovieList(String sessionId, String listId) throws MovieDbException {
        ApiUrl apiUrl = new ApiUrl(apiKey, BASE_LIST, listId);

        apiUrl.addArgument(PARAM_SESSION, sessionId);

        URL url = apiUrl.buildUrl();
        String webpage = requestWebPage(url, null, true);

        try {
            return MAPPER.readValue(webpage, StatusCode.class);
        } catch (IOException ex) {
            LOG.warn("Failed to delete movie list: {}", ex.getMessage());
            throw new MovieDbException(MovieDbException.MovieDbExceptionType.MAPPING_FAILED, webpage, ex);
        }
    }
}