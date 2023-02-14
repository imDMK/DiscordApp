package me.dmk.app.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.dmk.app.configuration.DatabaseConfiguration;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by DMK on 06.12.2022
 */

@RequiredArgsConstructor
public class MongoService {

    private final DatabaseConfiguration databaseConfiguration;

    @Getter
    private MongoClient mongoClient;
    @Getter
    private MongoDatabase mongoDatabase;

    private final Gson gson = new GsonBuilder()
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();

    public void connect() {
        boolean auth = databaseConfiguration.isAuthentication();
        String userName = databaseConfiguration.getUserName();
        String password = databaseConfiguration.getPassword();
        String hostName = databaseConfiguration.getHostName();
        int port = databaseConfiguration.getPort();
        String databaseName = databaseConfiguration.getDatabaseName();

        String connectUrl = "mongodb://" + (auth ? userName + ":" + password + "@" : "") + hostName + ":" + port + "/" + databaseName;

        this.mongoClient = new MongoClient(new MongoClientURI(connectUrl));
        this.mongoDatabase = this.mongoClient.getDatabase(databaseName);
    }

    public <T> T load(Document document, Class<T> tClass) {
        String json = document.toJson();
        return this.gson.fromJson(json, tClass);
    }

    public <V> List<V> loadAll(MongoCollection<Document> mongoCollection, Class<V> vClass) {
        List<V> vList = new ArrayList<>();

        mongoCollection.find().forEach((Consumer<? super Document>) document ->
                vList.add(this.load(document, vClass)));

        return vList;
    }

    public <V> void save(MongoCollection<Document> mongoCollection, Bson filters, V entity) {
        String json = this.gson.toJson(entity);
        Document document = Document.parse(json);

        if (document == null) {
            return;
        }

        mongoCollection.replaceOne(filters, document, new ReplaceOptions().upsert(true));
    }
}
