package com.smartats.engine;

import com.smartats.model.Application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class TopKSelector {

    public static class SelectionPartition {
        private final List<Application> selected = new ArrayList<>();
        private final List<Application> rejected = new ArrayList<>();

        public List<Application> getSelected() {
            return selected;
        }

        public List<Application> getRejected() {
            return rejected;
        }
    }

    /**
     * Partitions candidate applications into Top-K selected and remainder rejected
     * using a PriorityQueue Max-Heap ordered by ATS Match Score descending.
     */
    public static SelectionPartition partitionTopK(List<Application> applications, int kLimit) {
        SelectionPartition partition = new SelectionPartition();
        if (applications == null || applications.isEmpty()) {
            return partition;
        }

        // Custom Comparator: highest ATS score first; if equal, earlier application date first
        Comparator<Application> maxHeapComparator = (a1, a2) -> {
            int scoreCompare = Double.compare(a2.getCalculatedAtsScore(), a1.getCalculatedAtsScore());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            if (a1.getAppliedAt() != null && a2.getAppliedAt() != null) {
                return a1.getAppliedAt().compareTo(a2.getAppliedAt());
            }
            return 0;
        };

        PriorityQueue<Application> maxHeap = new PriorityQueue<>(maxHeapComparator);
        maxHeap.addAll(applications);

        int count = 0;
        while (!maxHeap.isEmpty()) {
            Application app = maxHeap.poll();
            if (count < kLimit) {
                app.setStatus("SELECTED");
                partition.getSelected().add(app);
            } else {
                app.setStatus("REJECTED");
                partition.getRejected().add(app);
            }
            count++;
        }

        return partition;
    }
}
