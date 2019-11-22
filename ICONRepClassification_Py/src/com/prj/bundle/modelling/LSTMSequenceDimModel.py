'''
Created on Dec 13, 2018

@author: iasl
'''
import sys
import numpy as np
from keras.models import Model, Sequential
from keras.layers import Dense, LSTM, Input, Flatten
from keras.optimizers import Adam
from keras.layers.embeddings import Embedding
from keras.layers.advanced_activations import LeakyReLU, PReLU
from tensorflow import set_random_seed
from numpy.random import seed

class LSTMSequenceDimModel:
    
    def __init__(self):
        self.instanceSpan = 0
        self.featureDimension = 0
        self.x_train = np.array([])
        
    def lstmModelConfiguration(self):
        
        hybridFeedDimension = self.featureDimension
        
        sequentialLayeredLSTM = Sequential()
        sequentialLayeredLSTM.add(LSTM(hybridFeedDimension, return_sequences=True, input_shape=(self.instanceSpan, self.featureDimension)))
 
#        sequentialLayeredLSTM.add(Flatten())
        sequentialLayeredLSTM.add(Dense(100, activation='relu'))
        sequentialLayeredLSTM.add(Dense(1, activation='sigmoid'))
#        sequentialLayeredLSTM.add(Dense(1, activation='tanh'))
#        sequentialLayeredLSTM.add(Dense(1, activation='linear'))
#        sequentialLayeredLSTM.add(LeakyReLU(alpha=0.001))
#         
#         
#        print("prior input shape>>",self.x_train.shape)
        transientStateScores = np.array(sequentialLayeredLSTM.predict(self.x_train))
#        print("output transient shape>>",transientStateScores.shape)
 
        return(transientStateScores)
        

seed(1)
set_random_seed(2)
