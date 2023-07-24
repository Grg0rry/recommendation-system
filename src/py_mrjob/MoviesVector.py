#!/usr/bin/env python3

from mrjob.job import MRJob
from mrjob.step import MRStep

class MovieVector(MRJob):
    
    def configure_args(self):
        super(MovieVector, self).configure_args()
        self.add_file_arg('--file1', type=str)

    def mapper_init(self):
        self.UserList = []

        with open(self.options.file1, 'r') as file1:
            for line in file1:
                line = line.strip().replace('"','').split('\t',1)
                self.UserList.append(int(line[1]))

    def mapper(self, _, line):
        line = line.strip().replace('"','').split('\t', 1)

        MovieTitle = str(line[0])
        UserRating = []
        for pair in line[1].strip('[]').split(','):
            UserID, Rating = pair.split(':')
            UserRating.append((int(UserID), int(Rating)))
        yield MovieTitle, UserRating

    def reducer(self, MovieTitle, UserRatings):        

        if len(list(UserRatings)) < 1000:
            return
        
        UserRatingsByOrder = {Order_UserID: 0 for Order_UserID in self.UserList}

        for UserRating in list(UserRatings):
            for UserID, Rating in UserRating:
                if UserID in UserRatingsByOrder:
                    UserRatingsByOrder[UserID] = int(Rating)

        Vector = list(UserRatingsByOrder.values())
        yield (MovieTitle, Vector)

    def steps(self):
        return [
            MRStep(mapper_init=self.mapper_init, 
                   mapper=self.mapper,
                   reducer=self.reducer)
        ]

if __name__ == '__main__':
    MovieVector.run()


