'''
Created on Dec 12, 2018

@author: iasl
'''
import sys
import numpy as np
from keras.models import Model, Sequential
from keras.layers import Dense, LSTM, Input
from keras.optimizers import Adam
from keras.layers.embeddings import Embedding
from tensorflow import set_random_seed
from numpy.random import seed

class SequentialLearning:
    
    def __init__(self):
        self.instanceSpan = 0
        self.featureDimension = 0
        self.x_train = np.array([])
        
    def lstmModelConfiguration(self):
        
        hybridFeedDimension = self.featureDimension
        
        modelInput = Input(shape = (self.instanceSpan,self.featureDimension))
        sequentialLayeredLSTM = Sequential()
        sequentialLayeredLSTM.add(LSTM(hybridFeedDimension, return_sequences=True, input_shape=(self.instanceSpan, self.featureDimension)))
        
#         sequentialLayeredLSTM.add(Dense(10, activation='relu'))
#         sequentialLayeredLSTM.add(Dense(2, activation='sigmoid'))
#         
#         lrAdam = Adam(lr=0.01,decay=0.001)
#         sequentialLayeredLSTM.compile(loss='binary_crossentropy', optimizer=lrAdam, metrics=['accuracy'])
        
        #sequentialLayeredLSTM.summary()
        
        
#         
        print("prior input shape>>",self.x_train.shape)
        transientStateScores = np.array(sequentialLayeredLSTM.predict(self.x_train))
        print(" transient shape>>",transientStateScores.shape)
         
        print(" final shape>>",transientStateScores)
        sys.exit()
        return(transientStateScores)
        
        return(sequentialLayeredLSTM)

seed(1)
set_random_seed(2)