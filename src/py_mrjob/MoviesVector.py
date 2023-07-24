#!/usr/bin/env python3

from mrjob.job import MRJob
from mrjob.step import MRStep
# import pydoop.hdfs as hdfs

class MovieVector(MRJob):
    
    def configure_args(self):
        super(MovieVector, self).configure_args()
        self.add_file_arg('--file1', type=str)

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.UserList = []
        self.MovieRating = {}

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
        
        if len(UserRating) < 1000:
            return
        else:
            self.MovieRating[MovieTitle] = UserRating
            yield MovieTitle, ""

    def reducer(self, MovieTitle, value):
        UserRatings = self.MovieRating[MovieTitle]
        
        UserRatingsByOrder = {Order_UserID: 0 for Order_UserID in self.UserList}

        for UserRating in UserRatings:
            for UserID, Rating in UserRating:
                if UserID in UserRatingsByOrder:
                    UserRatingsByOrder[UserID] = int(Rating)

        Vector = list(UserRatingsByOrder.values())
        yield (MovieTitle, Vector)


if __name__ == '__main__':
    MovieVector.run()


