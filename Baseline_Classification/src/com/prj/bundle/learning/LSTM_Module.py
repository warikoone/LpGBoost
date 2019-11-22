'''
Created on Mar 8, 2019

@author: iasl
'''

import os, sys, re, json, glob, nltk
import numpy as np
from tensorflow import set_random_seed
from numpy.random import seed
from keras.models import Model, Sequential
from keras.layers import Dense, LSTM, Input, Bidirectional, TimeDistributed, Flatten, Conv1D, Dropout
from keras.optimizers import Adam, SGD
from keras.layers.pooling import MaxPool1D
from keras.layers.advanced_activations import LeakyReLU

class LSTM_Module:
    
    def __init__(self):
        
        self.EMDEDDING_DIM = 0
        self.INSTANCE_SPAN = 0
        self.x_train = np.array([])
        self.y_train = np.array([])
        
        print('load LSTM Learning Module')
        
    
    def lstmModelConfiguration(self):
        
        tier1LayerDimension = self.EMDEDDING_DIM
        tier2LayerDimension = self.INSTANCE_SPAN
        modelInput = Input(shape = (self.INSTANCE_SPAN, self.EMDEDDING_DIM))
        lstmSequential = Sequential()
        lstmSequential.add((LSTM(tier1LayerDimension, return_sequences=True, input_shape=(self.INSTANCE_SPAN, self.EMDEDDING_DIM))))
        lstmSequential.add(Dense(1, activation='relu'))
        lstmSequential.add(Flatten())
        lstmSequential.add(Dense(2, activation='sigmoid'))
        #lstmSequential.add(LeakyReLU(alpha=0.01))
        
        #lrAdam = Adam(lr=0.01,decay=0.001)
        lrSgd = SGD(lr=0.01, decay=0.001)
        lrAdam = Adam(lr=0.01)
        lstmSequential.compile(optimizer= lrAdam, loss='binary_crossentropy',metrics=['accuracy'])
        
        return(lstmSequential)
        
        
seed(1)
set_random_seed(2)
