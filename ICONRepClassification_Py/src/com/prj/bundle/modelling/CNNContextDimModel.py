'''
Created on Dec 14, 2018

@author: iasl
'''

import sys
import numpy as np
from keras.models import Model, Sequential
from keras.layers import Dense, Input, Flatten, Conv1D, MaxPool1D, merge, concatenate
from keras.optimizers import Adam
from keras.layers.embeddings import Embedding
from keras.layers.advanced_activations import LeakyReLU, PReLU
from tensorflow import set_random_seed
from numpy.random import seed

class CNNContextDimModel:
    
    def __init__(self):
        self.instanceSpan = 0
        self.featureDimension = 0
        self.x_train = np.array([])
    
    def populateArray(self, currArray,appendArray):
    
        if currArray.shape[0] == 0:
            currArray = appendArray
        else:
            currArray = np.insert(currArray, currArray.shape[0], appendArray, 0)
        return(currArray)  
    
    def clubContextScores(self, tier1NdArray, tier2NdArray):
        
        if(tier1NdArray.shape[0] == 0):
            tier1NdArray = tier2NdArray
        else:
            tier3NdArray = np.array([])
            for tier1Index in range(tier2NdArray.shape[0]):
                for tier2Index in range(tier2NdArray.shape[1]):
                    bufferNdArray = tier1NdArray[tier1Index][tier2Index]
                    bufferNdArray = np.array([self.populateArray(self, bufferNdArray, tier2NdArray[tier1Index][tier2Index])])
                    tier3NdArray = self.populateArray(self, tier3NdArray, bufferNdArray)
            
            tier1NdArray = np.reshape(tier3NdArray, (tier3NdArray.shape[0], 1, tier3NdArray.shape[1]))
            
        return(tier1NdArray)
    
    def cnnModelConfiguration(self):
        
        #print("input shape>>",self.x_train.shape)
        filterSize = list(np.arange(2,self.instanceSpan+1))
        completeContextScore = np.array([])
        for eachFilter in filterSize:
            #print("kernel>>",eachFilter)
            contextSequential = Sequential()
            eachFilter = int(eachFilter)
            contextSequential.add(Conv1D(filters=self.featureDimension, kernel_size=eachFilter, padding='valid',activation='relu',strides=1, input_shape=(self.instanceSpan,self.featureDimension)))
            contextSequential.add(MaxPool1D(pool_size = 1))
            contextSequential.add(Dense(1, activation='relu'))
            #contextSequential.summary()
            subContextScore = np.array(contextSequential.predict(self.x_train))
            subContextScore = np.transpose(subContextScore, (0,2,1))
            completeContextScore = self.clubContextScores(self, completeContextScore, subContextScore)

        #print("output shape>>",completeContextScore.shape)
        
        return(completeContextScore)


seed(1)
set_random_seed(2)

