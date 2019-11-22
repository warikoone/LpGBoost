'''
Created on Jan 21, 2019

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

from src.com.prj.bundle.modelling import LSTMSequenceModel, CNNContextDimModel, DNNLearning
from numba.targets.arrayobj import populate_array
from sphinx.util.pycompat import sys_encoding

class Tier7Learning:

    def __init__(self):
        print("Loading Tier2Learning()")
        self.x_instance = {}
        self.y_instance = {}
        self.x_TestInstance = {}
        self.y_TestInstance = {}
        self.testInstanceId = {}
        self.kFoldSplit = 10
        self.channelSpan = 0
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
        
    def vectorizeDecisionStatus(self, bufferArray):
    
        vectorizedStatus = np.array([])
        for index in range(len(bufferArray)):
            statusValue = bufferArray[index]
            statusList = np.array([1.0,1.0])
            statusList = to_categorical(self.category_index[statusValue], len(self.category_index))
            vectorizedStatus = self.populateArray(vectorizedStatus, np.array([statusList]))
        return(vectorizedStatus)
    
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
    
    def populateVerticalArray(self, currArray,appendArray):
    
        if currArray.shape[0] == 0:
            currArray = appendArray
            currArray = np.reshape(currArray, (currArray.shape[1], 1))
        else:
            currArray = np.insert(currArray, currArray.shape[1], appendArray, 1)
            
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
    
    def assemble_XData(self, bufferArray, resourceType):
        
        tier1BufferArray = list()
        for index in range(len(bufferArray)):
            if resourceType == 1:
                tier1BufferArray.append(self.x_TestInstance.get(bufferArray[index]))
            elif resourceType == 2:
                tier1BufferArray.append(self.x_instance.get(bufferArray[index]))
        
        return(tier1BufferArray)
    
    def reshape4DArary(self, bufferArray, resourceType):

        tier2BufferNdArray = np.array([])        
        for index in range(len(bufferArray)):
            if resourceType == 1:
                tier1BufferNdArray = self.x_TestInstance.get(bufferArray[index])

            elif resourceType == 2:
                tier1BufferNdArray = self.x_instance.get(bufferArray[index])
          
            #print(np.array([tier1BufferNdArray]).shape)            
            tier2BufferNdArray = self.populateArray(tier2BufferNdArray, np.array([tier1BufferNdArray]))
            
        #print(tier2BufferNdArray.shape)
            
        return(tier2BufferNdArray)
    
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
    
    def adjustFeatureSpace(self, x_train):
        
        tier1BufferNdResultArray = np.array([])
        for tier1Index in range(x_train.shape[0]):
            tier2BufferNdResultArray = np.array([])
            tier1BufferNdArray = x_train[tier1Index]
            for tier2Index in range(tier1BufferNdArray.shape[0]):
                tier2BufferNdArray = tier1BufferNdArray[tier2Index]
                tier3BufferNdArray = np.array([np.nanmax(tier2BufferNdArray, axis=1)])
                tier2BufferNdResultArray = self.populateArray(tier2BufferNdResultArray, tier3BufferNdArray)
         
            #print(tier1Index,'\t',tier2BufferNdResultArray)   
            tier1BufferNdResultArray = self.populateArray(tier1BufferNdResultArray, np.array([tier2BufferNdResultArray]))
        
        #print(tier1BufferNdResultArray)
        #sys.exit()
        return(tier1BufferNdResultArray)
    
    def call_DNNConfiguration(self, x_train):
        
        dnnInstance = DNNLearning.DNNLearning
        dnnInstance.channelSpan = self.channelSpan
        dnnInstance.instanceSpan = self.instanceSpan
        dnnInstance.featureDimension = self.featureDimension
        #x_train = self.adjustFeatureSpace(x_train)
        dnnInstance.x_train = x_train
        dnnModel = dnnInstance.dnnModelConfiguration(dnnInstance)

        return(dnnModel, x_train)
    
    def voteForPrediction(self, probabilityScore):
        
        returnVal = -1
        #print(probabilityScore)
        if probabilityScore[0] <= probabilityScore[1]:
            returnVal = 1
            
        return(returnVal)
        
    
    def crossValidationSplit(self):
        
        passIndex = 1
        y_FoldPredictDict = {}
        y_FoldGoldDict = {}
        validationIndexDict, testIndexDict = self.classwiseValidation()
        for passIndex in validationIndexDict.keys():
            validIndex = validationIndexDict.get(passIndex)
            testIndex = list(self.x_TestInstance.keys())
            #print("\npassIndex>>",passIndex,"\n train>>",validIndex,"\n\t test>>",testIndex)
            print("\npassIndex>>",passIndex)
            x_test = self.reshape4DArary(testIndex, 1)
            y_testList = list(map(lambda valueItem: int(valueItem), self.reshape2DArray(testIndex, 1)))
            y_test = self.vectorizeDecisionStatus(y_testList)
            #self.checkForTransformationOverlap(validIndex)
            tier1ResultBufferDict = self.batchStratification(validIndex)
            y_BatchPredictDict = {}
            for keyTerm, valueTerm in tier1ResultBufferDict.items():
                print("\nsubpassIndex>>",keyTerm)
                x_train = self.reshape4DArary(valueTerm, 2)
                y_trainList = list(map(lambda valueItem: int(valueItem), self.reshape2DArray(valueTerm, 2)))
                y_train = self.vectorizeDecisionStatus(y_trainList)
                
                "add model configuration"       
                "DNN"
                dnnModel, x_TrainScores = self.call_DNNConfiguration(x_train)
                x_TestScores = x_test #self.adjustFeatureSpace(x_test)
                
                
                print("\n\t train",x_TrainScores.shape,"\t",y_train.shape)
                print("\n\t test",x_TestScores.shape,"\t",y_test.shape)
                
                
                ''' SVM '''
                '''
                svmKernel = svm.SVC(C=1, kernel='rbf',coef0=1.0 , degree=1, gamma=0.001)
                svmKernel.fit(x_TrainScores,y_trainList)
                y_predict = svmKernel.predict(x_TestScores)
                '''
                
                trainBatchSize = int(np.rint(x_TrainScores.shape[0]/2))
                x_validation = np.array(x_TrainScores[0:trainBatchSize])
                y_validation = np.array(y_train[0:trainBatchSize])
                
                epochSize = 5
                dnnModel.fit(x_TrainScores,y_train, batch_size = trainBatchSize, epochs = epochSize, verbose=0,validation_data=(x_validation,y_validation))
                score = dnnModel.evaluate(x_validation, y_validation, verbose=0)
                
                print(score)

                y_predict = list()
                for testPredictIndex in range(x_TestScores.shape[0]):
                    testExample = x_TestScores[testPredictIndex].reshape(1, self.channelSpan, self.instanceSpan, self.featureDimension)
                    probability = dnnModel.predict(testExample, verbose=2)
                    print(testPredictIndex,'\t',probability)
                    y_predict.append(self.voteForPrediction(probability[0]))
                
                print(classification_report(y_testList, y_predict))
                sys.exit()
               
               
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
                status = int(max(decoyVoteDictionary.items(),key=itemgetter(1))[0])
                #print(keyTerm,"\t",keyValue,"\t>>",status,"\t\t\t>>",self.y_instance[keyTerm])
                y_BatchPredict.append(status)
                tier1BufferList = []
                if y_FoldPredictDict.__contains__(keyTerm):
                    tier1BufferList = (y_FoldPredictDict.get(keyTerm))
                tier1BufferList.append(status)
                y_FoldPredictDict.update({keyTerm:tier1BufferList})
                y_FoldGoldDict.update({keyTerm:self.y_TestInstance[keyTerm]})
                
            print(classification_report(y_testList, y_BatchPredict))
            passIndex = passIndex+1
            #sys.exit()
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
            if keyValue == 1:
                if(self.testInstanceId.get(keyTerm)):
                    tier1BufferDict = self.testInstanceId.get(keyTerm)
                    for instanceId, instanceString in tier1BufferDict.items():
                        tier1BufferList = []
                        #print(instanceId)
                        docList = list(str(instanceId).split(sep="@"))
                        if y_finalInstancePrediction.__contains__(docList[0]):
                            tier1BufferList = y_finalInstancePrediction.get(docList[0])
                        tier1BufferList.append(docList[1]+"\t"+str(instanceString))
                        y_finalInstancePrediction.update({docList[0]:tier1BufferList})
          

        outFileName = self.openConfigurationFile("predictFilePath")
        fWriteBuffer = open(outFileName,'w')
        for keyTerm, keyValue in y_finalInstancePrediction.items():
            for sentence in keyValue:
                fWriteBuffer.write(keyTerm+"\t"+sentence+'\n')
        fWriteBuffer.close()
        return ()
       
    def readTrainingResourceData(self,fileDataPath):
        
        breakCounter = 0
        instanceList = list(self.category_index.keys())
        tier1BufferDict = dict()
        with open(fileDataPath, "r") as bufferFile:
            currentData = bufferFile.readline()
            index=0
            while len(currentData)!=0:
                #print(index)
                currentData = str(currentData).strip()
                decoyList = currentData.split(sep="\t")
                if len(decoyList) == 3:
                    instanceKey = int(decoyList[0])
                    docId = str(decoyList[1])
                    #sentIndex = int(decoyList[2])
                    '''
                    if instanceKey == -1 and breakCounter < 1000:
                        breakCounter = breakCounter+1
                        currentData = bufferFile.readline()
                        continue
                    '''
                    
                    #print(instanceKey,'\t',docId,'\t',sentIndex)
                    tier2BufferDict = {}
                    if tier1BufferDict.__contains__(instanceKey):
                        tier2BufferDict = tier1BufferDict.get(instanceKey)
                    
                    channelNdArray = np.array([])
                    if tier2BufferDict.__contains__(docId):
                        channelNdArray = tier2BufferDict.get(docId)
                        
                    vectorInstance = np.array([])
                    for valueItem in str(decoyList[2]).split(sep=","):
                        subVectorInstance = [float64(valueItem)]
                        '''
                        for subValueItem in valueItem.split(sep=","):
                            subVectorInstance.append(float64(subValueItem))
                        '''

                        vectorInstance = self.populateArray(vectorInstance, np.array([subVectorInstance]))
                    
                    
                    channelNdArray = self.populateArray(channelNdArray, np.array([vectorInstance]))
                    tier2BufferDict.update({docId:channelNdArray})
                    tier1BufferDict.update({instanceKey:tier2BufferDict})

                
                index = index+1
                currentData = bufferFile.readline()
          
        index = 0
        for tier1Key, tier1Value in tier1BufferDict.items():
            instanceKey = tier1Key
            for tier2Key, tier2Value in tier1Value.items():
                docId = tier2Key
                self.x_instance.update({index:tier2Value})
                self.y_instance.update({index:instanceKey})
                if(index == 0):
                    #print("zerodoc>>",docId)
                    self.channelSpan = tier2Value.shape[0]
                    self.instanceSpan = tier2Value.shape[1]
                    self.featureDimension = tier2Value.shape[2]
                    
                index = index+1
        
        
        print("channelSpan size>>",self.channelSpan)
        print("instanceSpan size>>",self.instanceSpan)
        print("featureDimension size>>",self.featureDimension)
        print("instance >>",len(self.x_instance))
        #sys.exit()
        return()
    
    
    def readTestResourceData(self,fileDataPath, testInstanceDict):
        
        instanceList = list(self.category_index.keys())
        tier1BufferDict = dict()
        with open(fileDataPath, "r") as bufferFile:
            currentData = bufferFile.readline()
            index=0
            while len(currentData)!=0:
                #print(index)
                currentData = str(currentData).strip()
                decoyList = currentData.split(sep="\t")
                if len(decoyList) == 3:
                    instanceKey = int(decoyList[0])
                    docId = str(decoyList[1])
                    #sentIndex = int(decoyList[2])
                    #print(instanceKey,'\t',docId,'\t',sentIndex)
                    tier2BufferDict = {}
                    if tier1BufferDict.__contains__(instanceKey):
                        tier2BufferDict = tier1BufferDict.get(instanceKey)
                    
                    channelNdArray = np.array([])
                    if tier2BufferDict.__contains__(docId):
                        channelNdArray = tier2BufferDict.get(docId)
                        
                    vectorInstance = np.array([])
                    for valueItem in str(decoyList[2]).split(sep=","):
                        subVectorInstance = [float64(valueItem)]
                        '''
                        for subValueItem in valueItem.split(sep=","):
                            subVectorInstance.append(float64(subValueItem))
                        '''
                        
                        vectorInstance = self.populateArray(vectorInstance, np.array([subVectorInstance]))
                    
                    
                    channelNdArray = self.populateArray(channelNdArray, np.array([vectorInstance]))
                    tier2BufferDict.update({docId:channelNdArray})
                    tier1BufferDict.update({instanceKey:tier2BufferDict})

                index = index+1
                currentData = bufferFile.readline()
                
        index = 0
        for tier1Key, tier1Value in tier1BufferDict.items():
            instanceKey = tier1Key
            for tier2Key, tier2Value in tier1Value.items():
                docId = tier2Key
                self.x_TestInstance.update({index:tier2Value})
                self.y_TestInstance.update({index:instanceKey})
                tier4BufferDict = {}
                if testInstanceDict.__contains__(instanceKey):
                    tier4BufferDict = testInstanceDict.get(instanceKey)
                tier5BufferDict = {}
                if tier4BufferDict.__contains__(docId):
                    tier5BufferDict.update({docId:tier4BufferDict.get(docId)})
                if len(tier5BufferDict) > 0:
                    self.testInstanceId.update({index:tier5BufferDict})
                else:
                    print("resource error in test instance ",docId)
                    sys.exit(0)
                    
                index = index+1

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
learningInstance = Tier7Learning()
learningInstance.loadResource()



