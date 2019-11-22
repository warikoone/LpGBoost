'''
Created on Dec 20, 2018

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

class SequentialConvolution:
    
    def __init__(self):
        self.instanceSpan = 0
        self.featureDimension = 0
        self.x_train = np.array([])
        
    def sequentialConvolveConfiguration(self):
        
        sequentialLayeredLSTM = Sequential()
        sequentialLayeredLSTM.add(Dense(100, activation='relu'))
#        sequentialLayeredLSTM.add(Dense(100, activation='relu'))
        sequentialLayeredLSTM.add(Dense(1, activation='sigmoid'))
        
#        print("prior input shape>>",self.x_train.shape)
        transientContextScores = np.array(sequentialLayeredLSTM.predict(self.x_train))
#        print("output transient shape>>",transientContextScores.shape)
#        print("prior input shape>>",self.x_train.shape)

        ''''
        transientContextScores = np.average(self.x_train, 2)
        transientContextScores = np.reshape(transientContextScores, (transientContextScores.shape[0],1,transientContextScores.shape[1]))
        '''
        '''
        print("output transient shape>>",transientContextScores.shape)
        for index in range(0,transientContextScores.shape[0]):
            print(index,"\t",transientContextScores[index][0][0])
        
        sys.exit()
        '''
        return(transientContextScores)
        

seed(1)
set_random_seed(2)