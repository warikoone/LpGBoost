'''
Created on Jan 22, 2019

@author: iasl
'''

import sys
import numpy as np
from keras.models import Model, Sequential
from keras.layers import Dense, Input, Flatten, Conv1D, MaxPool1D, merge, concatenate, Dropout, LSTM, Bidirectional
from keras.layers.merge import Concatenate
from keras.optimizers import Adam, SGD
from keras.layers.embeddings import Embedding
from keras.layers.advanced_activations import LeakyReLU, PReLU
from tensorflow import set_random_seed
from numpy.random import seed
from datashape.coretypes import float64
from keras.layers.wrappers import TimeDistributed

class DNNLearning:
    
    def __init__(self):
        self.channelSpan = 0
        self.instanceSpan = 0
        self.featureDimension = 0
        self.x_train = np.array([])
    
    def populateArray(self, currArray,appendArray):
    
        if currArray.shape[0] == 0:
            currArray = appendArray
        else:
            currArray = np.insert(currArray, currArray.shape[0], appendArray, 0)
        return(currArray)  
    
    
    def dnnModelConfiguration(self):
        
        print("input shape>>",self.x_train.shape)
        #modelInput = Input(shape =(self.channelSpan, self.instanceSpan,self.featureDimension))
        gradientFeatureLayerSize = self.featureDimension
        layerPass = False
        featureConnectedLayer = Sequential()
        #featureConnectedLayer.add(Dropout(0.2))
        #featureConnectedLayer.add(TimeDistributed((LSTM(12, activation="relu")), input_shape =(self.channelSpan, self.instanceSpan, self.featureDimension)))
        featureConnectedLayer.add(TimeDistributed(Flatten(), input_shape =(self.channelSpan, self.instanceSpan, self.featureDimension)))
        featureConnectedLayer.add(Flatten())
        
        '''
        featureConnectedLayer.add(TimeDistributed(Dense(gradientFeatureLayerSize,  activation='selu'), input_shape =(self.channelSpan, self.instanceSpan, self.featureDimension)))
        while(gradientFeatureLayerSize > 1):
            reductionRate = gradientFeatureLayerSize*0.60
            reductionRate = round(reductionRate)
            if reductionRate == 0:
                reductionRate = 1
            gradientFeatureLayerSize = (gradientFeatureLayerSize - reductionRate)
            if((not layerPass) and (gradientFeatureLayerSize < 1)):
                gradientFeatureLayerSize = 1
                layerPass = True
            featureConnectedLayer.add(TimeDistributed(Dense(gradientFeatureLayerSize, activation='selu')))
        
        featureConnectedLayer.add(TimeDistributed(Flatten()))

        gradientInstanceLayerSize = self.instanceSpan
        while(gradientInstanceLayerSize > 2):
            gradientInstanceLayerSize = gradientInstanceLayerSize-1
            featureConnectedLayer.add(TimeDistributed(Dense(gradientInstanceLayerSize, activation='selu')))
        featureConnectedLayer.add(Flatten())
        featureConnectedLayer.add(Dense(2, activation='tanh'))
        '''
        #featureConnectedLayer.add(Dense(4, activation='tanh'))
        featureConnectedLayer.add(Dense(2, activation='tanh'))
        
        
        featureConnectedLayer.summary()
        print(featureConnectedLayer.predict(self.x_train)[0:10])
        print(featureConnectedLayer.predict(self.x_train)[2500:2510])   
        sys.exit()
        
        
        lrAdam = Adam(lr=0.01,decay=0.001)
        lrSgd = SGD(lr=0.01, decay=0.001)
        #lrAdam = Adam(lr=0.01)
        featureConnectedLayer.compile(optimizer=lrSgd, loss='binary_crossentropy',metrics=['accuracy'])
        
        return(featureConnectedLayer)


seed(1)
set_random_seed(2)