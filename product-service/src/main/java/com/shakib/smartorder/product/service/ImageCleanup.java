package com.shakib.smartorder.product.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.shakib.smartorder.product.storage.ImageStorage;

// Keeps files and database rows in step: files are removed only when the database change really happened.
final class ImageCleanup {

    private ImageCleanup() {
    }
    // Delete the file after the transaction commits (the row that pointed at it is gone).
    static void deleteAfterCommit(ImageStorage storage, String url) 
    {
        if (url == null)
        {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() 
        {
            @Override
            public void afterCommit()
            {
                storage.delete(url);
            }
        });
    }

    // Delete a freshly stored file if the transaction rolls back, so no orphan file is left behind.
    static void deleteIfRolledBack(ImageStorage storage, String url) 
    {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
        {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED)
                {
                    storage.delete(url);
                }
            }
        });
    }
}
