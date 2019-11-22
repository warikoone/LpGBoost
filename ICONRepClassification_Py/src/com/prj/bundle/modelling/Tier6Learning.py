'''
Created on Jan 9, 2019

@author: iasl
'''
import numpy as np
from sklearn.model_selection import KFold
from keras.utils import to_categorical
from sklearn import svm
from sklearn.naive_bayes import GaussianNB,MultinomialNB
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
import xgboost as xgb
from sklearn.model_selection import GridSearchCV

from src.com.prj.bundle.modelling import LSTMSequenceModel, CNNContextDimModel


class Tier6Learning:

    def __init__(self):
        print("Loading Tier2Learning()")
        self.x_instance = {}
        self.y_instance = {}
        self.x_TestInstance = {}
        self.y_TestInstance = {}
        self.testInstanceId = {}
        self.kFoldSplit = 10
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
    
    
    def reshape3DArray(self, bufferArray, resourceType):

        tier2BufferArray = np.array([])        
        for index in range(len(bufferArray)):
            if resourceType == 1:
                tier1BufferArray = np.array([self.x_TestInstance.get(bufferArray[index])]).reshape((self.instanceSpan, self.featureDimension))
            elif resourceType == 2:
                tier1BufferArray = np.array([self.x_instance.get(bufferArray[index])]).reshape((self.instanceSpan, self.featureDimension))
                
            tier1BufferArray = np.array([tier1BufferArray])
            tier2BufferArray = self.populateArray(tier2BufferArray, tier1BufferArray)

        return(tier2BufferArray)
    
    def reshape2DArray(self, bufferArray, resourceType):
        
        tier2BufferArray = np.array([])        
        for index in range(len(bufferArray)):
            if resourceType == 1:
                statusList = np.array([[self.y_TestInstance.get(bufferArray[index])]])
            elif resourceType == 2:
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
        
        sequentialInstance = LSTMSequenceModel.LSTMSequenceModel
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
        x_trainTransientStateScores = np.reshape(x_trainTransientStateScores, (x_trainTransientStateScores.shape[0], x_trainTransientStateScores.shape[1]))
        contextualInstance.x_train = x_test
        x_testTransientStateScores = contextualInstance.cnnModelConfiguration(contextualInstance)
        x_testTransientStateScores = np.reshape(x_testTransientStateScores, (x_testTransientStateScores.shape[0], x_testTransientStateScores.shape[1]))
        
        return(x_trainTransientStateScores, x_testTransientStateScores)
    
    
    def crossValidationSplit(self):
        
        passIndex = 1
        y_FoldPredictDict = {}
        y_FoldGoldDict = {}
        validationIndexDict, testIndexDict = self.classwiseValidation()
        testBatch = int(np.rint(len(self.x_TestInstance)/self.kFoldSplit))
        startTest = 0
        endTest = 0
        for passIndex in validationIndexDict.keys():
            validIndex = validationIndexDict.get(passIndex)

            ''' within corpus - using the entire training data-set'''
            ''' detection'''
            #testIndex = testIndexDict.get(passIndex)
            
            ''' classification'''
            '''
            testIndex = list()
            for index, value in enumerate(self.x_TestInstance):
                if not (list(validIndex).__contains__(index)):
                    testIndex.append(index)
            '''
            
            ''' within corpus - using partial training set'''
            '''
            endTest = startTest+testBatch
            if endTest > len(self.x_TestInstance):
                endTest = len(self.x_TestInstance)
            testIndex = list(self.x_TestInstance.keys())[startTest:endTest]
            startTest = endTest
            '''
            
            ''' cross corpus'''
            testIndex = list(self.x_TestInstance.keys())
            
            
            #print("\npassIndex>>",passIndex,"\n train>>",validIndex,"\n test>>",testIndex)
            #print("\n valid>>",testIndex)
            print("\npassIndex>>",passIndex,'\t',len(validIndex),'\t',len(testIndex))
            x_test = self.reshape3DArray(testIndex, 1)
            y_test = list(map(lambda valueItem: int(valueItem), self.reshape2DArray(testIndex, 1)))
            #sys.exit()
            self.checkForTransformationOverlap(validIndex)
            tier1ResultBufferDict = self.batchStratification(validIndex)
            y_BatchPredictDict = {}
            for keyTerm, valueTerm in tier1ResultBufferDict.items():
                print("\nsubpassIndex>>",keyTerm)
                '''
                x_train = self.reshape3DArray(validIndex, 2)
                y_train = list(map(lambda valueItem: int(valueItem), self.reshape2DArray(validIndex, 2)))
                '''
                
                x_train = self.reshape3DArray(valueTerm, 2)
                y_train = list(map(lambda valueItem: int(valueItem), self.reshape2DArray(valueTerm, 2)))
                
                "Simple Representation"
                x_trainTransientStateScores = np.reshape(x_train, (x_train.shape[0], x_train.shape[1]))
                x_testTransientStateScores = np.reshape(x_test, (x_test.shape[0], x_test.shape[1]))
                
                "add model configuration"       
                "LSTM"
                #x_trainTransientStateScores, x_testTransientStateScores = self.call_LSTMConfiguration(x_train, x_test)         
                
                "CNN"
                #x_trainTransientStateScores, x_testTransientStateScores = self.call_CNNConfiguration(x_train, x_test) 
                
                print("\n\t train",x_trainTransientStateScores.shape,"\t",len(y_train))
                print("\n\t test",x_testTransientStateScores.shape,"\t",len(y_test))
                
                ''' SVM '''
                '''
                svmKernel = svm.SVC(C=1, kernel='rbf',coef0=1.0 , degree=1, gamma=10.0)
                svmKernel.fit(x_trainTransientStateScores,y_train)
                y_predict = svmKernel.predict(x_testTransientStateScores)
                '''
                
                ''' XGBoost'''
                # cross-corpus = max_depth=10, learning_rate=0.01
                # within-corpus = max_depth=10, learning_rate=1.0
                xg_class = xgb.XGBClassifier(max_depth=3, learning_rate=0.1, objective='binary:logistic', n_estimators=100, verbosity=2)
                xg_class.fit(x_trainTransientStateScores, y_train)
                y_predict =xg_class.predict(x_testTransientStateScores)
                
                
                ''' NAIVE BAYES'''
                '''
                gnb = GaussianNB()
                gnb.fit(x_trainTransientStateScores, y_train)
                y_predict = gnb.predict(x_testTransientStateScores)
                '''
                
                for index in range(len(y_predict)):
                    tier1BufferList = []
                    currIndex = testIndex[index]
                    #print(currIndex)
                    if y_BatchPredictDict.__contains__(currIndex):
                        tier1BufferList = y_BatchPredictDict.get(currIndex)
                    tier1BufferList.append(y_predict[index])
                    y_BatchPredictDict.update({currIndex:tier1BufferList})
                
                #break;
            
            #sys.exit()   
            y_BatchPredict = [] 
            for keyTerm, keyValue in y_BatchPredictDict.items():
                decoyVoteDictionary = dict(Counter(keyValue))
                status = int(max(decoyVoteDictionary.items(),key=itemgetter(1))[0])
                #print(keyTerm,"\t",keyValue,"\t>>",status,"\t\t\t>>",self.y_TestInstance[keyTerm])
                y_BatchPredict.append(status)
                tier1BufferList = []
                if y_FoldPredictDict.__contains__(keyTerm):
                    tier1BufferList = (y_FoldPredictDict.get(keyTerm))
                tier1BufferList.append(status)
                y_FoldPredictDict.update({keyTerm:tier1BufferList})
                y_FoldGoldDict.update({keyTerm:self.y_TestInstance[keyTerm]})
                
            print(classification_report(y_test, y_BatchPredict))
            passIndex = passIndex+1
            
            
        y_FoldBatchPredict = {} 
        for keyTerm, keyValue in y_FoldPredictDict.items():
            decoyVoteDictionary = dict(Counter(keyValue))
            status = int(max(decoyVoteDictionary.items(),key=itemgetter(1))[0])
            #print(keyTerm,"\t",keyValue,"\t>>",status,"\t\t\t>>",self.y_instance[keyTerm])
            y_FoldBatchPredict.update({keyTerm:status})
            
        y_gold = list(map(lambda valueItem : valueItem, y_FoldGoldDict.values()))
        y_predict = list(map(lambda valueItem : valueItem, y_FoldBatchPredict.values()))
        print(classification_report(y_gold, y_predict))
        
        y_finalInstancePrediction = {}
        for keyTerm, keyValue in y_FoldBatchPredict.items():
            if keyValue == 1 :
                if(self.testInstanceId.get(keyTerm)):
                    tier1BufferDict = self.testInstanceId.get(keyTerm)
                    for instanceId, instanceString in tier1BufferDict.items():
                        tier1BufferList = []
                        #print(instanceId)
                        docList = list(str(instanceId).split(sep="@"))
                        sentInd = docList[1].split(sep='#')[0]
                        if y_finalInstancePrediction.__contains__(docList[0]):
                            tier1BufferList = y_finalInstancePrediction.get(docList[0])
                        tier1BufferList.append(str(sentInd)+"\t"+str(instanceString))
                        y_finalInstancePrediction.update({docList[0]:tier1BufferList})
          

        outFileName = self.openConfigurationFile("predictFilePath")
        fWriteBuffer = open(outFileName,'w')
        for keyTerm, keyValue in y_finalInstancePrediction.items():
            for sentence in keyValue:
                fWriteBuffer.write(keyTerm+"\t"+sentence+'\n')
        fWriteBuffer.close()
        
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
                    vectorInstance = []
                    for valueItem in str(decoyList[2]).split(sep=","):
                        vectorInstance.append(float64(valueItem))
                    '''
                    instanceSize = len(vectorInstance)
                    sumVal = sum(vectorInstance)
                    sumVal = sumVal/instanceSize
                    vectorInstance = []
                    vectorInstance.append(float64(sumVal))
                    '''
                    
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
        
        return()
    
    
    def readTestResourceData(self,fileDataPath, testInstanceDict):
        
        with open(fileDataPath, "r") as bufferFile:
            currentData = bufferFile.readline()
            index=0
            while len(currentData)!=0:
                currentData = str(currentData).strip()
                decoyList = currentData.split(sep="\t")
                if len(decoyList) == 3:
                    instanceKey = int(decoyList[0])
                    vectorInstance = []
                    for valueItem in str(decoyList[2]).split(sep=","):
                        vectorInstance.append(float64(valueItem))
                    '''
                    instanceSize = len(vectorInstance)
                    sumVal = sum(vectorInstance)
                    sumVal = sumVal/instanceSize
                    vectorInstance = []
                    vectorInstance.append(float64(sumVal))
                    '''
                    
                    self.x_TestInstance.update({index:vectorInstance})
                    self.y_TestInstance.update({index:instanceKey})
                    tier1BufferDict = {}
                    if testInstanceDict.__contains__(instanceKey):
                        tier1BufferDict = testInstanceDict.get(instanceKey)
                    tier2BufferDict = {}
                    if tier1BufferDict.__contains__(decoyList[1]):
                        tier2BufferDict.update({decoyList[1]:tier1BufferDict.get(decoyList[1])})
                    if len(tier2BufferDict) > 0:
                        self.testInstanceId.update({index:tier2BufferDict})
                    else:
                        print("resource error in test instance ",decoyList[1])
                        sys.exit(0)
                    #print(currentData,"\t",index)
                    index = index+1
                currentData = bufferFile.readline()

        
        return()
    
    def readTestResource(self, testDataPath):
        
        tier1BufferResultDict = {}
        with open(testDataPath, "r") as bufferFile:
            currentData = bufferFile.readline()
            while len(currentData)!=0:
                tier1BufferList = str(currentData).split(sep="\t")
                testClassInstance = int(tier1BufferList[0])
                docId = str(tier1BufferList[1])
                testInstance = str(tier1BufferList[2]).strip()
                tier1BufferDict = {}
                if tier1BufferResultDict.__contains__(testClassInstance):
                    tier1BufferDict = tier1BufferResultDict.get(testClassInstance)
                tier1BufferDict.update({docId:testInstance})
                tier1BufferResultDict.update({testClassInstance:tier1BufferDict})
                currentData = bufferFile.readline()
            
        return(tier1BufferResultDict)
        
    def loadResource(self):
        
        testDataPath = self.openConfigurationFile("corpusTestInstanceFile")
        testInstanceDict = self.readTestResource(testDataPath)
        repTestDataPath = self.openConfigurationFile("corpusIconTestPath")
        self.readTestResourceData(repTestDataPath, testInstanceDict)
        
        repTrainDataPath = self.openConfigurationFile("corpusIconTrainingPath")
        self.readTrainingResourceData(repTrainDataPath)
        self.crossValidationSplit()
        return()
        

seed(1)
set_random_seed(2)
learningInstance = Tier6Learning()
learningInstance.loadResource()


