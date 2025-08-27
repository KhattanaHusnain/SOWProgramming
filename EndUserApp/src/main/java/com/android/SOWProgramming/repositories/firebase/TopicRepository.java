package com.android.SOWProgramming.repositories.firebase;

import com.android.SOWProgramming.models.Topic;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TopicRepository {
    FirebaseFirestore firestore;
    List<Topic> topics;
    public TopicRepository() {
        firestore = FirebaseFirestore.getInstance();
        topics = new ArrayList<>();
    }
    public interface Callback {
        void onSuccess(List<Topic> topics);
        void onFailure(String message);
    }
    public void loadTopicsOfCourse(int courseId, Callback callback) {
        firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Topics")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Topic topic = doc.toObject(Topic.class);
                        topics.add(topic);
                    }
                    if (!topics.isEmpty()) {
                        callback.onSuccess(topics);
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
