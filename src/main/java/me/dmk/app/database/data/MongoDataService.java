package me.dmk.app.database.data;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dmk.app.database.MongoClientService;
import me.dmk.app.database.data.entity.DataEntity;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by DMK on 23.02.2023
 */

@Slf4j
@RequiredArgsConstructor
public class MongoDataService {

    private final MongoClientService mongoClientService;
    private final Gson gson;

    private final Map<Class<?>, String> collections = new ConcurrentHashMap<>();

    public MongoCollection<Document> getCollection(Class<?> clazz) {
        String collection = this.collections.computeIfAbsent(
                clazz, c -> clazz.isAnnotationPresent(DataEntity.class) ? clazz.getAnnotation(DataEntity.class).collection() : clazz.getSimpleName()
        );

        return this.mongoClientService.getMongoDatabase().getCollection(collection);
    }

    public <V> List<V> findAll(String collectionName, Class<V> vClass) {
        MongoCollection<Document> mongoCollection = this.mongoClientService.getMongoDatabase().getCollection(collectionName);

        ArrayList<V> classes = new ArrayList<>();

        mongoCollection.find().forEach((Consumer<? super Document>) document -> {
            String json = document.toJson();
            V entity = this.gson.fromJson(json, vClass);

            classes.add(entity);
        });

        return classes;
    }

    public <V> void save(Bson filters, V entity) {
        MongoCollection<Document> mongoCollection = this.getCollection(entity.getClass());

        if (mongoCollection == null) {
            log.error("Cannot find collection from class " + entity.getClass().getSimpleName() + ", check entity annotation.");
            return;
        }

        String json = this.gson.toJson(entity);
        Document document = Document.parse(json);

        if (document == null) {
            log.error("Error while trying to parse document from json: " + json);
            return;
        }

        mongoCollection.replaceOne(filters, document, new ReplaceOptions().upsert(true));
    }

    public <V> boolean delete(V vClass) {
        String json = this.gson.toJson(vClass);
        Document document = Document.parse(json);

        if (document == null) {
            log.error("Error while trying to parse document from json: " + json);
            return false;
        }

        MongoCollection<Document> mongoCollection = this.getCollection(vClass.getClass());

        if (mongoCollection == null) {
            log.error("Cannot find collection from class " + vClass.getClass().getSimpleName() + ", check entity annotation.");
            return false;
        }

        return mongoCollection.deleteOne(document).wasAcknowledged();
    }
}
