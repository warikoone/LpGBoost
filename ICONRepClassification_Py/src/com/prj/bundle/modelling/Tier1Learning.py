'''
Created on Dec 12, 2018

@author: iasl
'''
import numpy as np
from sklearn.model_selection import KFold
from keras.utils import to_categorical
from sklearn import svm
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

from src.com.prj.bundle.modelling import SequentialLearning

class Tier1Learning:
    
    def __init__(self):
        print("Loading Tier1Learning()")
        self.x_instance = {}
        self.y_instance = {}
        self.kFoldSplit = 5
        self.instanceSpan = 0
        self.featureDimension = 1
        self.category_index = {-1:0, 1:1}


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
        
        tier1Int = max(tier1BufferDict.items(), key=itemgetter(1))[0]
        tier2Int = min(tier1BufferDict.items(), key=itemgetter(1))[0]
        batchFold = tier1BufferDict.get(tier2Int)
        batchFactor = math.ceil(tier1BufferDict.get(tier1Int)/tier1BufferDict.get(tier2Int))
        #print("\n\t min >>",tier1BufferDict.get(tier2Int),"\t", batchFactor)
        
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
            startIndex = 0
            if(bufferKey != minClass):
                tier1BufferList = tier1BufferDict.get(bufferKey)
                endIndex = startIndex+batchFold
                factor=1
                count = 0
                #print(len(tier1BufferList))
                while (count < batchFactor):
                    #print("start\t",startIndex,"end\t",endIndex,"factor\t",factor)
                    tier2BufferList = []
                    for index in range(startIndex, endIndex, factor):
                        #print(index)
                        tier2BufferList.append(tier1BufferList[index])
                    #print(tier2BufferList)
                    tier2BufferList.extend(tier1BufferDict.get(minClass))
                    tier1ResultBufferDict.update({count:tier2BufferList})
                    temp = (endIndex+batchFold)
                    #print("\t\t\t\t",temp)
                    if(temp > len(tier1BufferList)):
                        startIndex = (len(tier1BufferList)-batchFold)
                        endIndex = len(tier1BufferList)
                        factor = +1
                    else:
                        startIndex = endIndex
                        endIndex = temp
                        
                    count = count+1
                        
        #sys.exit()
        return(tier1ResultBufferDict)
    
    def populateArray(self, currArray,appendArray):
    
        if currArray.shape[0] == 0:
            currArray = appendArray
        else:
            currArray = np.insert(currArray, currArray.shape[0], appendArray, 0)
        return(currArray)  
    
    def vectorizeDecisionStatus(self,statusValue):
        
        
        statusList = np.array([0.0,0.0])
        if statusValue != "na":
            statusList = np.array([to_categorical(self.category_index[statusValue], len(self.category_index))])

        return(statusList)
    
    def reshape3DArray(self, bufferArray):

        tier2BufferArray = np.array([])        
        for index in range(len(bufferArray)):
            tier1BufferArray = np.array([self.x_instance.get(bufferArray[index])]).reshape((self.instanceSpan, self.featureDimension))
            tier1BufferArray = np.array([tier1BufferArray])
            tier2BufferArray = self.populateArray(tier2BufferArray, tier1BufferArray)

        return(tier2BufferArray)
    
    def reshape2DArray(self, bufferArray):
        
        tier2BufferArray = np.array([])        
        for index in range(len(bufferArray)):
            statusList = self.vectorizeDecisionStatus(self.y_instance.get(bufferArray[index]))
            #statusList = self.y_instance.get(bufferArray[index])
            statusList = np.array([statusList])
            #print(statusList.shape)
            tier2BufferArray = self.populateArray(tier2BufferArray, statusList)
            
        return(tier2BufferArray)
    
    def scoreProbabilityPair(self, bufferArray):
        
        retVal = 1
        if(bufferArray[0] > bufferArray[1]):
            retVal = self.category_index[-1]
            
        return(retVal)
    
    def crossValidationSplit(self):
        
        kFoldSplitStrata = KFold(n_splits = self.kFoldSplit)
        tier1BufferArary = np.array(list(map(lambda valueItem : int(valueItem), self.x_instance.keys())))
        passIndex = 1
        y_FoldPredictDict = {}
        y_FoldGoldDict = {}
        for validIndex, testIndex in kFoldSplitStrata.split(tier1BufferArary):
            #print("\npassIndex>>",passIndex,"\n train>>",validIndex,"\t test>>",testIndex)
            print("\npassIndex>>",passIndex)
            testIndexList = list(map(lambda valueItem : int(valueItem), testIndex))
            x_test = self.reshape3DArray(testIndexList)
            y_test = self.reshape2DArray(testIndexList)
            #y_test = list(map(lambda valueItem: int(valueItem), self.reshape2DArray(testIndexList)))
            tier1ResultBufferDict = self.batchStratification(validIndex)
            y_BatchPredictDict = {}
            for keyTerm, valueTerm in tier1ResultBufferDict.items():
                #print("\nsubpassIndex>>",keyTerm)
                x_train = self.reshape3DArray(valueTerm)
                y_train = self.reshape2DArray(valueTerm)
                batchSize = int(y_train.shape[0]/3)
                x_valid = np.array(x_train[0:batchSize])
                y_valid = np.array(y_train[0:batchSize])
#                 print("\n\t train",x_train.shape,"\t",y_train.shape)
#                 print("\n\t valid",x_valid.shape,"\t",y_valid.shape)
#                 print("\n\t test",x_test.shape,"\t",y_test.shape)
                 
                "add model configuration"                
                sequentialInstance = SequentialLearning.SequentialLearning
                sequentialInstance.instanceSpan = self.instanceSpan
                sequentialInstance.featureDimension = self.featureDimension
                sequentialInstance.x_train = x_train
                model = sequentialInstance.lstmModelConfiguration(sequentialInstance)
                #y_train = y_train.reshape(y_train.shape[0],1,2)
                #y_test = y_test.reshape(y_test.shape[0],1,2)
                 
                model.fit(x_train,y_train, batch_size = batchSize, epochs=5, validation_data=(x_valid, y_valid), verbose=0)
                score = model.evaluate(x_valid, y_valid, verbose=0)
                #print(score)
                 
                for i in range(0,x_test.shape[0]):
                    testExample = x_test[i].reshape(1,self.instanceSpan,self.featureDimension)
                    instanceProb = model.predict(testExample,verbose=2)
                    tier1BufferList = []
                    if y_BatchPredictDict.__contains__(i):
                        tier1BufferList = y_BatchPredictDict.get(i)
                    tier1BufferList.append(self.scoreProbabilityPair(instanceProb[0][0]))
                    y_BatchPredictDict.update({i:tier1BufferList})
                 
#                 sequentialInstance.x_train = x_train
#                 x_transientStateScores = sequentialInstance.lstmModelConfiguration(sequentialInstance)
#                 sequentialInstance.x_train = x_test
#                 x1_transientStateScores = sequentialInstance.lstmModelConfiguration(sequentialInstance)
#                 svmKernel = svm.SVC(C=1.0, kernel='rbf',coef0=0.0 , degree=1, gamma=0.04)
#                 svmKernel.fit(x_transientStateScores,y_train)
#                 y_predict = svmKernel.predict(x1_transientStateScores)
            for keyTerm, keyValue in y_BatchPredictDict.items():
                decoyVoteDictionary = dict(Counter(keyValue))
                status = int(max(decoyVoteDictionary.items(),key=itemgetter(1))[0])
                y_FoldPredictDict.update({testIndexList[keyTerm]:status})
                y_FoldGoldDict.update({testIndexList[keyTerm]:self.category_index[self.y_instance[testIndexList[keyTerm]]]})
                 
            passIndex = passIndex+1
         
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
                decoyList = currentData.split(sep="\t", maxsplit=1)
                if len(decoyList) == 2:
                    instanceKey = int(decoyList[0])
                    vectorInstance = []
                    for valueItem in str(decoyList[1]).split(sep=","):
                        vectorInstance.append(float64(valueItem))
                    self.x_instance.update({index:vectorInstance})
                    self.y_instance.update({index:instanceKey})
                    if(index == 0):
                        self.instanceSpan = len(vectorInstance)
                    if(len(vectorInstance)<self.instanceSpan):
                        print(index)
                        sys.exit()
                    #print(currentData,"\t",index)
                    index = index+1
                currentData = bufferFile.readline()
                
        self.crossValidationSplit()
        return()
        
    def loadResource(self):
        
        repDataPath = self.openConfigurationFile("iconRepresentationPath")
        self.readTrainingResourceData(repDataPath)
        print(repDataPath)
        return()

seed(1)
set_random_seed(2)
learningInstance = Tier1Learning()
learningInstance.loadResource()
        