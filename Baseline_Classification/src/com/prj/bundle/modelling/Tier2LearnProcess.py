'''
Created on Mar 8, 2019

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
import sys, os, re, json, math
from tensorflow import set_random_seed
from numpy.random import seed
from scipy import float64
from gensim.models import KeyedVectors
from keras.preprocessing.text import text_to_word_sequence, Tokenizer
from keras.preprocessing.sequence import pad_sequences

#sys.path.append('/home/iasl/Neha_W/NeonWorkspace_1.3/Baseline_Classification/')

from src.com.prj.bundle.learning.CNN_Module import CNN_Module
from src.com.prj.bundle.learning.LSTM_Module import LSTM_Module
from src.com.prj.bundle.learning.BiLSTM_Module import BiLSTM_Module

class Tier2LearnProcess:

    def __init__(self):
        print("Loading Tier2LearnProcess()")
        self.x_instance = {}
        self.y_instance = {}
        self.x_TestInstance = {}
        self.y_TestInstance = {}
        self.trainWordMap = {}
        self.testWordMap = {}
        self.testInstanceId = {}
        self.kFoldSplit = 10
        self.category_index = {-1:0, 1:1}
        self.VOCABDICT = {}
        

        
    def openConfigurationFile(self,jsonVariable):
    
        currPath = os.path.dirname(sys.argv[0])
        if currPath == '':
            currPath = sys.path[0]
            print(currPath) 
            
        configPath = 'config.json'
        updatePath = []
        for pathMatch in re.split("\/", currPath):
            if re.match('src', pathMatch):
                break
            else:
                updatePath.append(pathMatch)
        updatePath.append(configPath)
        configPath = "/".join(updatePath)

        with open(configPath, "r") as json_file:
            data = json.load(json_file)
            jsonVariableValue = data[jsonVariable]
            json_file.close()
        
        if jsonVariableValue is not None:
            return(jsonVariableValue)
        else:
            print("\n\t Variable load failure")
            sys.exit()
            
        return()
        
    
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

        tier2BufferNdArray = np.array([])        
        for index in range(len(bufferArray)):
            if resourceType == 1:
                tier1BufferNdArray = self.x_TestInstance.get(bufferArray[index])
                
            elif resourceType == 2:
                tier1BufferNdArray = self.x_instance.get(bufferArray[index])
                    
            tier2BufferNdArray = self.populateArray(tier2BufferNdArray, np.array([tier1BufferNdArray]))

        #print(tier2BufferNdArray.shape)
        return(tier2BufferNdArray)
    
    def reshape2DArray(self, bufferArray, resourceType):
        
        tier1BufferNdArray = []        
        for index in range(len(bufferArray)):
            if resourceType == 1:
                tier1Value = self.y_TestInstance.get(bufferArray[index])
            elif resourceType == 2:
                tier1Value = self.y_instance.get(bufferArray[index])
                
            statusList = self.category_index[tier1Value]
            #print(statusList.shape)
            tier1BufferNdArray.append(statusList)
        
        #print(tier1BufferNdArray.shape)
        return(tier1BufferNdArray)
    
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
    
    
        
    def findTweetVectorEmbeddings(self):
        
        for tier1MapValue in self.resourceData.items():
            instanceType = tier1MapValue[0]
            tier2MapValue = dict(tier1MapValue[1])
            decoyEmbeddingDictionary = {}
            for tier3MapValue in tier2MapValue.items():
                decoyIndex = tier3MapValue[0]
                embeddingMatrix = self.assimilatePreTrainedEmbeddings(self, list(tier3MapValue[1]))
                decoyEmbeddingDictionary.update({decoyIndex:embeddingMatrix})
             
            self.TWEET_EMBEDDING.update({instanceType:decoyEmbeddingDictionary})   
        return()
    
    
    def generateResourceData(self, bufferInstanceDict, bufferInstanceList, bufferTokenizer):

        tier1InstanceList = bufferTokenizer.texts_to_matrix(bufferInstanceList, mode='tfidf')
        print(tier1InstanceList.shape)        
        tier1BufferDict= {}
        tier2BufferDict= {}
        for tier1Key, tier1Value in bufferInstanceDict.items():
            for tier2Key, tier2Value in tier1Value.items():
                #print(tier1Key,'\t',tier2Key,'\t',tier2Value,'\t',len(tier1InstanceList))
                tier1BufferDict.update({tier1Key:tier1InstanceList[tier1Key]})
                tier2BufferDict.update({tier1Key:tier2Value})
        
        return(tier1BufferDict, tier2BufferDict)
    
    def readRawResource(self, testDataPath):
        
        tier1BufferResultList = []
        tier1BufferResultDict = {}
        index = 0
        with open(testDataPath, "r") as bufferFile:
            currentData = bufferFile.readline()
            while len(currentData)!=0:
                tier1BufferList = []
                tier1BufferList = str(currentData).split(sep="\t")
                testClassInstance = int(tier1BufferList[0])
                docId = str(tier1BufferList[1])
                testInstance = str(tier1BufferList[2]).strip()
                tier1BufferDict = {}
                if tier1BufferResultDict.__contains__(index):
                    tier1BufferDict = tier1BufferResultDict.get(index)
                tier1BufferDict.update({docId:testClassInstance})
                tier1BufferResultDict.update({index:tier1BufferDict})
                tier1BufferResultList.append(testInstance)
                index = index+1
                currentData = bufferFile.readline()
        
        return(tier1BufferResultDict, tier1BufferResultList)
    
    
    def call_LSTMConfiguration(self, x_train):
        
        lstmInstance = LSTM_Module
        lstmInstance.INSTANCE_SPAN = self.INSTANCE_SPAN
        lstmInstance.EMDEDDING_DIM = self.EMBEDDING_DIM
        lstmInstance.x_train = x_train
        lstmModel = lstmInstance.lstmModelConfiguration(lstmInstance)

        return(lstmModel)

    def call_BiLSTMConfiguration(self, x_train):
        
        biLstmInstance = BiLSTM_Module
        biLstmInstance.INSTANCE_SPAN = self.INSTANCE_SPAN
        biLstmInstance.EMDEDDING_DIM = self.EMBEDDING_DIM
        biLstmInstance.x_train = x_train
        biLstmModel = biLstmInstance.biLSTMModelConfiguration(biLstmInstance)

        return(biLstmModel)
    
    def call_CNNConfiguration(self, x_train):

        cnnInstance = CNN_Module
        cnnInstance.INSTANCE_SPAN = self.INSTANCE_SPAN
        cnnInstance.EMDEDDING_DIM = self.EMBEDDING_DIM
        cnnInstance.x_train = x_train
        cnnModel = cnnInstance.cnnModelConfiguration(cnnInstance)
        
        return(cnnModel)
    
    def predictStatus(self, bufferList):
        
        status = 0
        if bufferList[0] < bufferList[1]:
            status = 1
            
        return(status)
    
    def crossValidationSplit(self):
        
        passIndex = 1
        y_FoldPredictDict = {}
        y_FoldGoldDict = {}
        validationIndexDict, testIndexDict = self.classwiseValidation()
        for passIndex in validationIndexDict.keys():
            validIndex = validationIndexDict.get(passIndex)
            ''' cross corpus '''
            #testIndex = list(self.x_TestInstance.keys())
            ''' within corpus '''
            testIndex = testIndexDict.get(passIndex)
            #print("\npassIndex>>",passIndex,"\n train>>",validIndex,"\t test>>",testIndex)
            print("\npassIndex>>",passIndex,'\t',testIndex)
            x_test = self.reshape3DArray(testIndex, 1)
            #y_test = list(map(lambda valueItem: int(valueItem), self.reshape2DArray(testIndex, 1)))
            y_test = self.reshape2DArray(testIndex, 1)
            #self.checkForTransformationOverlap(validIndex)
            tier1ResultBufferDict = self.batchStratification(validIndex)
            y_BatchPredictDict = {}
            for keyTerm, valueTerm in tier1ResultBufferDict.items():
                print("\nsubpassIndex>>",keyTerm)
                x_train = self.reshape3DArray(valueTerm, 2)
                #y_train = list(map(lambda valueItem: int(valueItem), self.reshape2DArray(valueTerm, 2)))
                y_train = self.reshape2DArray(valueTerm, 2)
                
                print("\n\t train",x_train.shape,"\t",len(y_train))
                print("\n\t test",x_test.shape,"\t",len(y_test))
                
                
                ''' SVM '''
                '''
                svmKernel = svm.SVC(C=1, kernel='rbf',coef0=1.0 , degree=1, gamma=0.1)
                svmKernel.fit(x_train,y_train)
                y_predict = svmKernel.predict(x_test)
                '''
                
                ''' NAIVE BAYES'''
                gnb = GaussianNB()
                gnb.fit(x_train, y_train)
                y_predict = gnb.predict(x_test)
                
                for index in range(len(y_predict)):
                    tier1BufferList = []
                    currIndex = testIndex[index]
                    if y_BatchPredictDict.__contains__(currIndex):
                        tier1BufferList = y_BatchPredictDict.get(currIndex)
                    tier1BufferList.append(y_predict[index])
                    y_BatchPredictDict.update({currIndex:tier1BufferList})
            
            y_Actual = []  
            y_BatchPredict = [] 
            for keyTerm, keyValue in y_BatchPredictDict.items():
                decoyVoteDictionary = dict(Counter(keyValue))
                status = int(max(decoyVoteDictionary.items(),key=itemgetter(1))[0])
                actualStatus = self.category_index.get(self.y_TestInstance[keyTerm])
                print(keyTerm,"\t",keyValue,"\t>>",status,"\t\t\t>>",actualStatus)
                y_BatchPredict.append(status)
                tier1BufferList = []
                if y_FoldPredictDict.__contains__(keyTerm):
                    tier1BufferList = (y_FoldPredictDict.get(keyTerm))
                tier1BufferList.append(status)
                y_FoldPredictDict.update({keyTerm:tier1BufferList})
                y_Actual.append(actualStatus)
                y_FoldGoldDict.update({keyTerm:actualStatus})
               
            print(classification_report(y_Actual, y_BatchPredict))
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
            if keyValue == 1:
                #print(keyTerm,'\t',keyValue)
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
    
    
    def generateInstanceMap(self, bufferInstanceDict, bufferInstanceList):
        
        for tier1Key, tier1Value in bufferInstanceDict.items():
            tier1BufferDict = {}
            for tier2Key, tier2Value in tier1Value.items():
                tier1BufferDict.update({tier2Key:bufferInstanceList[tier1Key]})
                
            self.testInstanceId.update({tier1Key:tier1BufferDict})
            
        return()
    
    def assembleVocab(self, bufferTier1InstanceList, bufferTier2InstanceList):
        
        bufferTier1List = []
        bufferTier1List.extend(bufferTier1InstanceList)
        bufferTier1List.extend(bufferTier2InstanceList)
        bufferTokenizer = Tokenizer()
        bufferTokenizer.fit_on_texts(bufferTier1List)
        self.VOCABDICT = bufferTokenizer.word_index
        
        return(bufferTokenizer)
    
    def loadResource(self):
        
        repTestDataPath = self.openConfigurationFile("corpusIconTestPath")
        testInstanceDict, testInstanceList  = self.readRawResource(repTestDataPath)
        self.generateInstanceMap(testInstanceDict, testInstanceList)
        
        repTrainDataPath = self.openConfigurationFile("corpusIconTrainingPath")
        trainInstanceDict, trainInstanceList = self.readRawResource(repTrainDataPath)
        
        bufferTokenizer = self.assembleVocab(testInstanceList, trainInstanceList)
        
        self.x_TestInstance, self.y_TestInstance = self.generateResourceData(testInstanceDict, testInstanceList, bufferTokenizer)
        self.x_instance, self.y_instance = self.generateResourceData(trainInstanceDict, trainInstanceList, bufferTokenizer)
        
        self.crossValidationSplit()
        
        return()
        

seed(1)
set_random_seed(2)
learningInstance = Tier2LearnProcess()
learningInstance.loadResource()

