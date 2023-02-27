package me.dmk.app.database;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.dmk.app.configuration.DatabaseConfiguration;

/**
 * Created by DMK on 06.12.2022
 */

@Getter
@RequiredArgsConstructor
public class MongoClientService {

    private final DatabaseConfiguration databaseConfiguration;

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;

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
}
