'''
Created on Mar 8, 2019

@author: iasl
'''

import os, sys, re, json, glob, nltk
import numpy as np
from tensorflow import set_random_seed
from numpy.random import seed
from keras.models import Model, Sequential
from keras.layers import Dense, LSTM, Input, Bidirectional, TimeDistributed, Flatten, Conv1D, Embedding
from keras.layers.merge import Concatenate
from keras.optimizers import Adam, SGD
from keras.layers.pooling import MaxPool1D
from keras.layers.advanced_activations import LeakyReLU
from tensorflow.python.framework.random_seed import set_random_seed

class CNN_Module:
    
    def __init__(self):
        
        self.EMDEDDING_DIM = 0
        self.INSTANCE_SPAN = 0
        self.embeddingMatrix = np.array([])
        self.x_train = np.array([])
        
        print('load CNN Learning Module')
        
    
    def cnnModelConfiguration(self):
        
        tier1LayerDimension = self.EMDEDDING_DIM
        tier2LayerDimension = self.INSTANCE_SPAN
        modelInput = Input(shape = (self.INSTANCE_SPAN, self.EMDEDDING_DIM))
        '''
        embedingLayer = Embedding((self.embeddingMatrix.shape[0]), # or len(word_index) + 1
                            self.embeddingMatrix.shape[1], # or EMBEDDING_DIM,
                            weights=[self.embeddingMatrix],
                            input_length=self.INSTANCE_SPAN,
                            trainable=True)(modelInput)
        '''
        filterBlock = [3]
        convBlocks = []
        
        for eachFilter in filterBlock:
            singleConv = Conv1D(filters=tier1LayerDimension,kernel_size=eachFilter,padding='valid',activation='relu',strides=1)(modelInput)
            singleConv = MaxPool1D(pool_size = 1)(singleConv)
            singleConv = Dense(1,activation='relu')(singleConv)
            singleConv = Flatten()(singleConv)
            convBlocks.append(singleConv)
            
        tranformLayer = Concatenate()(convBlocks) if len(convBlocks) > 1 else convBlocks[0]
        #tranformLayer = Dense(10,activation='relu')(tranformLayer)
        modelOutput = Dense(2,activation='sigmoid')(tranformLayer)
        cnnConcatSequence = Model(inputs = modelInput, outputs=modelOutput)

        #lrAdam = Adam(lr=0.01,decay=0.001)
        lrSgd = SGD(lr=0.01, decay=0.0001)
        lrAdam = Adam(lr=0.01)
        cnnConcatSequence.compile(optimizer= lrAdam, loss='binary_crossentropy',metrics=['accuracy'])

        return(cnnConcatSequence)
        
        
seed(1)
set_random_seed(2)

