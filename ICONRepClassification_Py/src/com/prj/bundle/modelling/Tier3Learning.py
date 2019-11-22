'''
Created on Dec 13, 2018

@author: iasl
'''

import numpy as np
from sklearn.model_selection import KFold
from keras.utils import to_categorical
from sklearn import svm
from sklearn.naive_bayes import GaussianNB
from sklearn.metrics import classification_report, precision_score, recall_score, f1_score
from collections import Counter
from operator import itemgetter
import sys
import os
import re
import json
import math
from tensorflow import set_random_seed
from numpy.random import seed
from scipy import float64

from src.com.prj.bundle.modelling import LSTMSequenceDimModel, CNNContextDimModel


class Tier3Learning:

    def __init__(self):
        print("Loading Tier2Learning()")
        self.x_instance = {}
        self.y_instance = {}
        self.kFoldSplit = 5
        self.instanceSpan = 0
        self.featureDimension = 0
        self.category_index = {-1:0, 1:1}
        self.windowSize = 0

        
    def openConfigurationFile(self,jsonVariable):
    
        path = os.path.dirname(sys.argv[0])
        tokenMatcher = re.search(".*ICONRepClassification_Py\/", path)
        if tokenMatcher:
            configFile = tokenMatcher.group(0)
            configFile="".join([configFile,"config.json"])
        
        jsonVariableValue = None
        with open(configFile, "r") as json_file:
            data = json.load(json_file)
            jsonVariableValue = data[jsonVariable]
            json_file.close()
            
        if jsonVariableValue is None:
            print("\n\t Variable load failure")
            sys.exit()
            
        return(jsonVariableValue)
        
    
    def accessClassImbalance(self, bufferDictionary):
        
        tier1BufferDict = {}
        for keyTerm, valueTerm in bufferDictionary.items():
            tier1BufferDict.update({keyTerm:len(valueTerm)});
        
        print(">>>>>>>",tier1BufferDict)
        tier1Int = max(tier1BufferDict.items(), key=itemgetter(1))[0]
        tier2Int = min(tier1BufferDict.items(), key=itemgetter(1))[0]
        batchFold = tier1BufferDict.get(tier2Int)
        batchFactor = math.ceil(tier1BufferDict.get(tier1Int)/tier1BufferDict.get(tier2Int))
        if((batchFactor%2)==0):
            batchFactor = batchFactor+1
        print("\n\t min >>",tier1BufferDict.get(tier2Int),"\t", batchFactor)
        
        return(batchFold, batchFactor, tier2Int)
    
    def batchStratification(self, bufferArray):
        
        tier1BufferDict = {}
        for bufferItem in bufferArray:
            itemVal = self.y_instance.get(bufferItem)
            tier1BufferList = []
            if(tier1BufferDict.__contains__(itemVal)):
                tier1BufferList = tier1BufferDict.get(itemVal);
            tier1BufferList.append(bufferItem)
            tier1BufferDict.update({itemVal:tier1BufferList})

        batchFold, batchFactor, minClass = self.accessClassImbalance(tier1BufferDict)
        tier1ResultBufferDict = {}
        for bufferKey in tier1BufferDict:
            if(bufferKey != minClass):
                tier1BufferList = tier1BufferDict.get(bufferKey)
                tier2BufferList = set(tier1BufferList)
                count = 0
                while (count < batchFactor):
                    tier1BufferArray = np.asarray(list(tier2BufferList))
                    tier3BufferList = set(map(lambda itemValue : int(itemValue), np.random.choice(tier1BufferArray, batchFold, False)))
                    tier4BufferList = list(tier3BufferList.union(tier1BufferDict.get(minClass)))
                    tier1ResultBufferDict.update({count:tier4BufferList})
                    tier2BufferList = set(tier2BufferList.difference(tier3BufferList))
                    if(len(tier2BufferList) < batchFold ):
                        tier2BufferList = set(tier1BufferList)
                    #print(len(tier3BufferList),"\t",len(tier4BufferList),"\t",len(tier2BufferList))
                    count = count+1
        
        return(tier1ResultBufferDict)
    
    def populateArray(self, currArray,appendArray):
    
        if currArray.shape[0] == 0:
            currArray = appendArray
        else:
            currArray = np.insert(currArray, currArray.shape[0], appendArray, 0)
        return(currArray)  
    
    
    def reshape3DArray(self, bufferArray):

        tier2BufferArray = np.array([])        
        for index in range(len(bufferArray)):
            tier1BufferArray = np.array([self.x_instance.get(bufferArray[index])])
            tier2BufferArray = self.populateArray(tier2BufferArray, tier1BufferArray)
            
        return(tier2BufferArray)
    
    def reshape2DArray(self, bufferArray):
        
        tier2BufferArray = np.array([])        
        for index in range(len(bufferArray)):
            statusList = np.array([[self.y_instance.get(bufferArray[index])]])
            tier2BufferArray = self.populateArray(tier2BufferArray, statusList)
        
        return(tier2BufferArray)
    
    def callKFoldSplit(self, bufferArray, tier1BufferDict, tier2BufferDict):
        
        kFoldSplitStrata = KFold(n_splits = self.kFoldSplit)
        passIndex = 0
        for validIndex, testIndex in kFoldSplitStrata.split(bufferArray):
            tier1BufferList = []
            if tier1BufferDict.__contains__(passIndex):
                tier1BufferList = tier1BufferDict.get(passIndex)
            tier3BufferList = list(map(lambda valueItem : bufferArray[valueItem], validIndex))
            tier1BufferList.extend(tier3BufferList)
            tier1BufferDict.update({passIndex:tier1BufferList})
            
            tier2BufferList = []
            if tier2BufferDict.__contains__(passIndex):
                tier2BufferList = tier2BufferDict.get(passIndex)
            tier3BufferList = list(map(lambda valueItem : bufferArray[valueItem], testIndex))
            tier2BufferList.extend(tier3BufferList)
            tier2BufferDict.update({passIndex:tier2BufferList})
            
            passIndex = passIndex+1
        
        return(tier1BufferDict, tier2BufferDict)
    
    def classwiseValidation(self):
        
        validationIndexDict = {}
        testIndexDict = {}
        tier1BufferArray = np.array(list(filter(lambda valueItem : (self.y_instance.get(valueItem) ==-1), self.y_instance.keys())))
        tier2BufferArray = np.array(list(filter(lambda valueItem : (self.y_instance.get(valueItem) == 1), self.y_instance.keys())))
        validationIndexDict, testIndexDict = self.callKFoldSplit(tier1BufferArray, validationIndexDict, testIndexDict )
        validationIndexDict, testIndexDict = self.callKFoldSplit(tier2BufferArray, validationIndexDict, testIndexDict )
        
        return(validationIndexDict, testIndexDict)
    
    def checkForTransformationOverlap(self, bufferArray):
        
        negArray = []
        posArray = []
        for index in bufferArray:
            if self.y_instance.get(index) == -1:
                negArray.append(index)
            else:
                posArray.append(index)

        index = 0
        totSize = len(posArray)
        mapArray = {}
        for index in range(len(posArray)):
            skip = False
            for pairKey in mapArray.keys():
                if posArray[index] in mapArray.get(pairKey):
                    skip = True
                    break
            if not skip:
                buffer = []
                for subIndex in range(index+1, len(posArray)):
                    if np.array_equal(self.x_instance.get(posArray[index]), self.x_instance.get(posArray[subIndex])):
                        buffer.append(posArray[subIndex])
                
                    mapArray.update({posArray[index]:buffer})
                
            
        negOverLap = {}
        for index in mapArray.keys():
            buffer = []
            if negOverLap.__contains__(index):
                buffer = negOverLap.get(index)
            for subIndex in negArray:
                if np.array_equal(self.x_instance.get(index), self.x_instance.get(subIndex)):
                    buffer.append(subIndex)
                    
            negOverLap.update({index:buffer})
        
        sizeCount = 0
        for itemVal in negOverLap.items():
            #print(itemVal)
            if(len(itemVal[1])>0):
                sizeCount = sizeCount+1
            
        print("OVERLAP IWITH NEG>>>>>",sizeCount)
        #sys.exit()
        return()
    
    def call_LSTMConfiguration(self, x_train, x_test):
        
        sequentialInstance = LSTMSequenceDimModel.LSTMSequenceDimModel
        sequentialInstance.instanceSpan = self.instanceSpan
        sequentialInstance.featureDimension = self.featureDimension
        sequentialInstance.x_train = x_train
        x_trainTransientStateScores = sequentialInstance.lstmModelConfiguration(sequentialInstance)
        x_trainTransientStateScores = np.reshape(x_trainTransientStateScores, (x_trainTransientStateScores.shape[0], x_trainTransientStateScores.shape[1]))
        sequentialInstance.x_train = x_test
        x_testTransientStateScores = sequentialInstance.lstmModelConfiguration(sequentialInstance)
        x_testTransientStateScores = np.reshape(x_testTransientStateScores, (x_testTransientStateScores.shape[0], x_testTransientStateScores.shape[1]))
        
        return(x_trainTransientStateScores, x_testTransientStateScores)
    
    def call_CNNConfiguration(self, x_train, x_test):
        
        contextualInstance = CNNContextDimModel.CNNContextDimModel
        contextualInstance.instanceSpan = self.instanceSpan
        contextualInstance.featureDimension = self.featureDimension
        contextualInstance.x_train = x_train
        x_trainTransientStateScores = contextualInstance.cnnModelConfiguration(contextualInstance)
        x_trainTransientStateScores = np.reshape(x_trainTransientStateScores, (x_trainTransientStateScores.shape[0], x_trainTransientStateScores.shape[2]))
        contextualInstance.x_train = x_test
        x_testTransientStateScores = contextualInstance.cnnModelConfiguration(contextualInstance)
        x_testTransientStateScores = np.reshape(x_testTransientStateScores, (x_testTransientStateScores.shape[0], x_testTransientStateScores.shape[2]))
        
        return(x_trainTransientStateScores, x_testTransientStateScores)
    
    def crossValidationSplit(self):
        
        passIndex = 1
        y_FoldPredictDict = {}
        y_FoldGoldDict = {}
        validationIndexDict, testIndexDict = self.classwiseValidation()
        for passIndex in validationIndexDict.keys():
            validIndex = validationIndexDict.get(passIndex)
            testIndex = testIndexDict.get(passIndex)
            #print("\npassIndex>>",passIndex,"\n train>>",validIndex,"\t test>>",testIndex)
            print("\npassIndex>>",passIndex)
            x_test = self.reshape3DArray(testIndex)
            y_test = list(map(lambda valueItem: int(valueItem), self.reshape2DArray(testIndex)))
            self.checkForTransformationOverlap(validIndex)
            tier1ResultBufferDict = self.batchStratification(validIndex)
            y_BatchPredictDict = {}
            for keyTerm, valueTerm in tier1ResultBufferDict.items():
                print("\nsubpassIndex>>",keyTerm)
                x_train = self.reshape3DArray(valueTerm)
                y_train = list(map(lambda valueItem: int(valueItem), self.reshape2DArray(valueTerm)))
                
                "Simple Representation"
                x_trainTransientStateScores = np.average(x_train, 2)
                #x_trainTransientStateScores = np.average(x_trainTransientStateScores,1)
                x_testTransientStateScores = np.average(x_test, 2)
                #x_testTransientStateScores = np.average(x_testTransientStateScores,1)
                #x_trainTransientStateScores = np.reshape(x_trainTransientStateScores, (x_trainTransientStateScores.shape[0], 1))
                #x_testTransientStateScores = np.reshape(x_testTransientStateScores, (x_testTransientStateScores.shape[0], 1))
                
                #x_trainTransientStateScores = np.reshape(x_train, (x_train.shape[0], x_train.shape[1]))
                #x_testTransientStateScores = np.reshape(x_test, (x_test.shape[0], x_test.shape[1]))
                
                "add model configuration"
                "LSTM"
                #x_trainTransientStateScores, x_testTransientStateScores = self.call_LSTMConfiguration(x_train, x_test)
                "CNN"
                #x_trainTransientStateScores, x_testTransientStateScores = self.call_CNNConfiguration(x_train, x_test)
                 
                print("\n\t train",x_trainTransientStateScores.shape,"\t",len(y_train))
                print("\n\t test",x_testTransientStateScores.shape,"\t",len(y_test))
                
                ''' SVM '''
                svmKernel = svm.SVC(C=1.0, kernel='rbf',coef0=1.0 , degree=1, gamma=0.01)
                svmKernel.fit(x_trainTransientStateScores,y_train)
                y_predict = svmKernel.predict(x_testTransientStateScores)
                
                ''' NAIVE BAYES'''
                '''
                gnb = GaussianNB()
                gnb.fit(x_trainTransientStateScores, y_train)
                y_predict = gnb.predict(x_testTransientStateScores)
                '''
                
                for index in range(len(y_predict)):
                    tier1BufferList = []
                    currIndex = testIndex[index]
                    if y_BatchPredictDict.__contains__(currIndex):
                        tier1BufferList = y_BatchPredictDict.get(currIndex)
                    tier1BufferList.append(y_predict[index])
                    y_BatchPredictDict.update({currIndex:tier1BufferList})
                     
                
            y_BatchPredict = [] 
            for keyTerm, keyValue in y_BatchPredictDict.items():
                decoyVoteDictionary = dict(Counter(keyValue))
                '''
                if len(decoyVoteDictionary) > 1:
                    status = int(min(decoyVoteDictionary.items(),key=itemgetter(1))[0])
                else:
                    status = int(max(decoyVoteDictionary.items(),key=itemgetter(1))[0])
                '''
                status = int(max(decoyVoteDictionary.items(),key=itemgetter(1))[0])
                #print(keyTerm,"\t",keyValue,"\t>>",status,"\t\t\t>>",self.y_instance[keyTerm])
                y_BatchPredict.append(status)
                y_FoldPredictDict.update({keyTerm:status})
                y_FoldGoldDict.update({keyTerm:self.y_instance[keyTerm]})
                
            print(classification_report(y_test, y_BatchPredict))
            passIndex = passIndex+1
            #sys.exit()
            
        y_gold = list(map(lambda valueItem : valueItem, y_FoldGoldDict.values()))
        y_predict = list(map(lambda valueItem : valueItem, y_FoldPredictDict.values()))
          
        print(classification_report(y_gold, y_predict))
        sys.exit()
        return ()
       
    def readTrainingResourceData(self,fileDataPath):
        
        with open(fileDataPath, "r") as bufferFile:
            currentData = bufferFile.readline()
            index=0
            while len(currentData)!=0:
                currentData = str(currentData).strip()
                decoyList = currentData.split(sep="\t")
                if len(decoyList) == 3:
                    instanceKey = int(decoyList[0])
                    vectorList = list(map(lambda valueItem : float64(valueItem), list(str(decoyList[2]).split(sep=","))))
                    rowSize = int(len(vectorList)/self.windowSize)
                    vectorInstance = np.array(vectorList)
                    vectorInstance = np.reshape(vectorInstance, (rowSize, self.windowSize))
                    self.x_instance.update({index:vectorInstance})
                    self.y_instance.update({index:instanceKey})
                    if(index == 0):
                        self.instanceSpan = vectorInstance.shape[0]
                        self.featureDimension = self.windowSize
                    if(vectorInstance.shape[0]<self.instanceSpan):
                        print(index)
                        sys.exit()
                    #print(currentData,"\t",index)
                    index = index+1
                currentData = bufferFile.readline()

        self.crossValidationSplit()
        return()
        
    def loadResource(self):
        
        repDataPath = self.openConfigurationFile("iconRepresentationPath")
        self.windowSize = int(self.openConfigurationFile("seedFrame"))
        self.readTrainingResourceData(repDataPath)
        print(repDataPath)
        return()
        

seed(1)
set_random_seed(2)
learningInstance = Tier3Learning()
learningInstance.loadResource()

